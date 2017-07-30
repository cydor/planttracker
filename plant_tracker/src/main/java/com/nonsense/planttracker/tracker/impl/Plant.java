package com.nonsense.planttracker.tracker.impl;

import com.nonsense.planttracker.tracker.interf.IPlantUpdateListener;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;

/**
 * Created by Derek Brooks on 6/30/2017.
 */

public class Plant implements Serializable {

    private static final long serialVersionUID = 2153335460648792L;

    public enum VegFlower   {
        Veg,
        Flower
    }

    private long plantId;
    private long parentPlantId;
    private String plantName;
    private Calendar startDate;
    private ArrayList<Recordable> recordableEvents;
    private VegFlower vegFlowerState;
    private Calendar flowerStartDate;
    private boolean isFromSeed;
    private boolean isArchived;
    private ArrayList<Long> groupIds;

    // TODO store grouping auto-complete as part of settings

    private transient ArrayList<IPlantUpdateListener> updateListeners;

    public Plant(Calendar growStartDate, String plantName, boolean isFromSeed) {
        this.plantName = plantName;
        plantId = System.currentTimeMillis();
        startDate = growStartDate;
        recordableEvents = new ArrayList<>();
        vegFlowerState = VegFlower.Veg;
        updateListeners = new ArrayList<>();
        this.isFromSeed = isFromSeed;
        groupIds = new ArrayList<>();
    }

    public void addUpdateListener(IPlantUpdateListener pul) {
        if (updateListeners == null)    {
            updateListeners = new ArrayList<>();
        }

        updateListeners.add(pul);
    }

    public String getPlantName()    {
        return plantName;
    }

    public void setPlantName(String name) {
        plantName = name;

        notifyUpdateListeners();
    }

    public void setParentPlantId(long id) {
        parentPlantId = id;
    }

    public long getParentPlantId()  {
        return parentPlantId;
    }

    public long getPlantId()    {
        return plantId;
    }

    public boolean isFromSeed()
    {
        return isFromSeed;
    }

    public int getRecordableEventCount()    {
        return recordableEvents.size();
    }

    public ArrayList<Recordable> getAllRecordableEvents()  {
        return recordableEvents;
    }

    public void startGrow(Calendar c) {
        long currentDay = getDaysFromStart();
        long currentWeek = getWeeksFromStart();

        recordableEvents.add(new EventRecord(currentDay, currentWeek,
                EventRecord.PlantEvent.GrowStart));

        notifyUpdateListeners();
    }

    public void feedPlant(double foodStrength, double pH)  {
        long currentDay = getDaysFromStart();
        long currentWeek = getWeeksFromStart();

        recordableEvents.add(new EventRecord(currentDay, currentWeek, EventRecord.PlantEvent.Food,
                foodStrength, pH));

        notifyUpdateListeners();
    }

    public void waterPlant(double pH)   {
        long currentDay = getDaysFromStart();
        long currentWeek = getWeeksFromStart();

        recordableEvents.add(new EventRecord(currentDay, currentWeek, EventRecord.PlantEvent.Water,
                0.0, pH));

        notifyUpdateListeners();
    }

    public void switchToFlower()    {
        long currentDay = getDaysFromStart();
        long currentWeek = getWeeksFromStart();

        recordableEvents.add(new EventRecord(currentDay, currentWeek,
                EventRecord.PlantEvent.FloweringState));

        setPlantState(VegFlower.Flower);

        flowerStartDate = Calendar.getInstance();

        notifyUpdateListeners();
    }

    public void switchToVeg()   {
        long currentDay = getDaysFromStart();
        long currentWeek = getWeeksFromStart();

        recordableEvents.add(new EventRecord(currentDay, currentWeek,
                EventRecord.PlantEvent.VegetationState));

        setPlantState(VegFlower.Veg);

        flowerStartDate = null;

        notifyUpdateListeners();
    }

    public VegFlower getVegFlowerState()    {
        return vegFlowerState;
    }

    public void addObservation(int rhHigh, int rhLow, int tempHigh, int tempLow, String notes)    {
        long currentDay = getDaysFromStart();
        long currentWeek = getWeeksFromStart();

        recordableEvents.add(new ObservationRecord(currentDay, currentWeek, rhHigh, rhLow,
                tempHigh, tempLow, notes));

        notifyUpdateListeners();
    }

    public long getDaysFromStart()   {
        return calcDaysFromTime(startDate);
    }

    public long getWeeksFromStart()   {
        return calcWeeksFromTime(startDate);
    }

    public long getDaysFromFlowerStart() {
        if (flowerStartDate == null)    {
            return 0;
        }

        return calcDaysFromTime(flowerStartDate);
    }

    public long getWeeksFromFlowerStart()   {
        if (flowerStartDate == null)    {
            return 0;
        }
        return calcWeeksFromTime(flowerStartDate);
    }

    public long calcDaysFromTime(Calendar start)    {
        Calendar end = Calendar.getInstance();

        Date startDate = start.getTime();
        Date endDate = end.getTime();
        long startTime = startDate.getTime();
        long endTime = endDate.getTime();
        long diffTime = endTime - startTime;
        long diffDays = diffTime / (1000 * 60 * 60 * 24);

        return diffDays + 1;    // shift to 1 based
    }

    public long calcWeeksFromTime(Calendar start)   {
        Calendar end = Calendar.getInstance();

        Date startDate = start.getTime();
        Date endDate = end.getTime();
        long startTime = startDate.getTime();
        long endTime = endDate.getTime();
        long diffTime = endTime - startTime;
        long diffDays = diffTime / (1000 * 60 * 60 * 24 * 7);

        return diffDays + 1;    // shift to 1 based
    }

    public void changePlantingDate(Calendar c)  {
        startDate = c;

        long currentDay = getDaysFromStart();
        long currentWeek = getWeeksFromStart();
        recordableEvents.add(new EventRecord(currentDay, currentWeek,
                EventRecord.PlantEvent.ChangePlantingDate, c));

        notifyUpdateListeners();
    }

    public void changeFloweringDate(Calendar c) {
        flowerStartDate = c;

        long currentDay = getDaysFromStart();
        long currentWeek = getWeeksFromStart();
        recordableEvents.add(new EventRecord(currentDay, currentWeek,
                EventRecord.PlantEvent.ChangeFloweringDate, c));

        notifyUpdateListeners();
    }

    public void addGeneralEvent(String generalEventName, String generalEventAbbrev,
                                String eventNotes)  {
        long currentDay = getDaysFromStart();
        long currentWeek = getWeeksFromStart();
        recordableEvents.add(new EventRecord(currentDay, currentWeek, generalEventName,
                generalEventAbbrev, eventNotes));

        notifyUpdateListeners();
    }

    public boolean isFlowering()    {
        return (vegFlowerState == VegFlower.Flower);
    }


    private void setPlantState(VegFlower state)    {
        vegFlowerState = state;
    }

    public Calendar getPlantStartDate() {
        return startDate;
    }

    public Calendar getFlowerStartDate() {
        return flowerStartDate;
    }

    private void notifyUpdateListeners()    {
        for(IPlantUpdateListener pul : updateListeners) {
            pul.plantUpdate(this);
        }
    }

    public void archivePlant() {
        isArchived = true;
        notifyUpdateListeners();
    }

    public void unarchivePlant()   {
        isArchived = false;
        notifyUpdateListeners();
    }

    public boolean isArchived() {
        return isArchived;
    }

    public void regeneratePlantId() {
        plantId = System.currentTimeMillis();
    }

    @Override
    public boolean equals(Object p)  {
        return ((Plant)p).getPlantId() == getPlantId();
    }

    public void addGroup(long groupId)  {
        if (!groupIds.contains(groupId))    {
            groupIds.add(groupId);
        }
    }

    public void removeGroup(long groupId)   {
        groupIds.remove(groupId);
    }

    public ArrayList<Long> getGroups() {
        return groupIds;
    }

}