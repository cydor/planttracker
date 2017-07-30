package com.nonsense.planttracker;

// Additional source attribution for icon:
// website http://www.freepik.com/free-icon/plant-growing_743982.htm

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.content.FileProvider;
import android.view.SubMenu;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import com.nonsense.planttracker.tracker.impl.EventRecord;
import com.nonsense.planttracker.tracker.impl.Group;
import com.nonsense.planttracker.tracker.impl.ObservationRecord;
import com.nonsense.planttracker.tracker.adapters.PlantRecordableTileArrayAdapter;
import com.nonsense.planttracker.tracker.adapters.PlantTileArrayAdapter;
import com.nonsense.planttracker.tracker.impl.Plant;
import com.nonsense.planttracker.tracker.impl.PlantTracker;
import com.nonsense.planttracker.tracker.impl.Recordable;

import org.w3c.dom.Text;

import java.io.File;
import java.io.FilenameFilter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Stack;
import java.util.TreeMap;


public class PlantTrackerUi extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private static String PT_FILE_EXTENSION = ".ser";

    private ViewSwitcher switcher;
    private LinearLayout allPlantsView;
    private LinearLayout individualPlantView;
    private Toolbar toolbar;

    // All plants view
    private ListView plantListView;

    // Individual plant view
    private TextView plantNameTextView;
    private TextView daysSinceGrowStartTextView;
    private TextView weeksSinceGrowStartTextView;
    private TextView daysSinceFlowerStartTextView;
    private TextView weeksSinceFlowerStartTextView;
    private TextView growStartTextView;
    private TextView fromSeedTextView;
    private ListView recordableEventListView;
    private CheckBox floweringCheckBox;
    private Button waterButton;
    private Button feedButton;
    private Button observationButton;
    private Button generalButton;
    private Menu individualPlantMenu;
    private SubMenu addToGroup;
    private SubMenu removeFromGroup;

    private PlantTileArrayAdapter plantTileAdapter;
    private PlantRecordableTileArrayAdapter plantRecordableAdapter;

    // Data
    private TreeMap<Integer, Long>  menuItemToGroupIdMapping = new TreeMap<>();
    private Stack<Plant> parentPlantViewStack;
    private ArrayList<Plant> currentDisplayArray;
    private PlantDisplay plantDisplay = PlantDisplay.Active;
    private PlantTracker tracker;
    private Plant currentPlant;
    private long groupIdViewFilter;

    private enum PlantDisplay   {
        All,
        Active,
        Archived,
        Group
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plant_tracker_ui);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setSubtitle("Showing Active Plants");

        switcher = (ViewSwitcher)findViewById(R.id.viewSwitcher);
        allPlantsView = (LinearLayout)findViewById(R.id.allPlantsView);
        individualPlantView = (LinearLayout)findViewById(R.id.individualPlantView);

        // all plants view
        plantListView = (ListView)findViewById(R.id.plantListView);

        bindIndividualPlantView();

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
            this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close) {

            public void onDrawerOpened(View drawerView) {
                drawerView.bringToFront();
            }
        };
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        parentPlantViewStack = new Stack<>();
        tracker = new PlantTracker(getFilesDir().toString());

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        refreshDrawerGroups();

        fillViewWithPlants();
    }

    private void bindIndividualPlantView()  {
        plantNameTextView = (TextView)findViewById(R.id.plantNameTextView);
        daysSinceGrowStartTextView = (TextView)findViewById(R.id.daysSinceGrowStartTextView);
        daysSinceFlowerStartTextView = (TextView)findViewById(R.id.daysSinceFlowerStartTextView);
        weeksSinceGrowStartTextView = (TextView)findViewById(R.id.weeksSinceGrowStartTextView);
        weeksSinceFlowerStartTextView = (TextView)findViewById(R.id.weeksSinceFlowerStartTextView);
        growStartTextView = (TextView)findViewById(R.id.growStartTextView);
        fromSeedTextView = (TextView)findViewById(R.id.fromSeedTextView);
        recordableEventListView = (ListView)findViewById(R.id.recordableEventListView);
        generalButton = (Button)findViewById(R.id.generalEventButton);

        generalButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                presentGeneralEventDialog();
            }
        });

        waterButton = (Button)findViewById(R.id.waterButton);
        waterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                presentWateringDialog();
            }
        });

        feedButton = (Button)findViewById(R.id.feedButton);
        feedButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
            presentFeedingDialog();
            }
        });

        observationButton = (Button)findViewById(R.id.observeButton);
        observationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                presentAddObservationDialog();
            }
        });

        floweringCheckBox = (CheckBox)findViewById(R.id.floweringCheckBox);
    }

    private void fillViewWithPlants()   {
        switch(plantDisplay)    {
            case All:
                currentDisplayArray = tracker.getAllPlants();
                toolbar.setSubtitle("Showing All Plants");
                break;
            case Active:
                currentDisplayArray = tracker.getActivePlants();
                toolbar.setSubtitle("Showing Active Plants");
                break;
            case Archived:
                currentDisplayArray = tracker.getArchivedPlants();
                toolbar.setSubtitle("Showing Archived Plants");
                break;
            case Group:
                Group g = tracker.getGroup(groupIdViewFilter);
                ArrayList<Plant> plants = tracker.getActivePlants();
                ArrayList<Plant> displayList = new ArrayList<>();
                for(Plant p : plants)   {
                    if (p.getGroups().contains(g.getGroupId())) {
                        displayList.add(p);
                    }
                }

                currentDisplayArray = displayList;
                toolbar.setSubtitle("Showing Group: " + g.getGroupName());
                break;
        }

        PlantTileArrayAdapter adapter = new PlantTileArrayAdapter(getBaseContext(),
                R.layout.plant_list_tile, currentDisplayArray);

        plantListView.setAdapter(adapter);
        plantListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                currentPlant = currentDisplayArray.get(position);
                toolbar.setSubtitle("");
                fillIndividualPlantView();
                switcherToNext();
            }
        });
    }

    private void fillIndividualPlantView()  {
        plantNameTextView.setText(currentPlant.getPlantName());
        daysSinceGrowStartTextView.setText("" + currentPlant.getDaysFromStart());
        daysSinceFlowerStartTextView.setText("" + currentPlant.getDaysFromFlowerStart());
        weeksSinceGrowStartTextView.setText("" + currentPlant.getWeeksFromStart());
        weeksSinceFlowerStartTextView.setText("" + currentPlant.getWeeksFromFlowerStart());
        growStartTextView.setText("" + formatDate(currentPlant.getPlantStartDate()));
        fromSeedTextView.setText("" + (currentPlant.isFromSeed()?"From Seed on":"Cloned on"));

        final Plant parentPlant = tracker.getPlantById(currentPlant.getParentPlantId());
        TextView parentPlantTextView = (TextView)findViewById(R.id.parentPlantIdTextView);
        parentPlantTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                parentPlantViewStack.push(currentPlant);
                currentPlant = parentPlant;
                fillIndividualPlantView();
            }
        });

        if (!currentPlant.isFromSeed() && currentPlant.getParentPlantId() > 0) {
            if (parentPlant == null)    {
                parentPlantTextView.setText("Parent Plant: " + parentPlant.getPlantName());
            }
            else    {
                // couldn't find the parent plant
                parentPlantTextView.setText("Parent Plant: " + currentPlant.getParentPlantId());
            }

            parentPlantTextView.setVisibility(View.VISIBLE);
        }
        else    {
            parentPlantTextView.setVisibility(View.INVISIBLE);
        }

        floweringCheckBox.setOnCheckedChangeListener(null);
        floweringCheckBox.setChecked(currentPlant.getVegFlowerState() == Plant.VegFlower.Flower);
        floweringCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b)  {
                    currentPlant.switchToFlower();
                    fillIndividualPlantView();
                }
                else    {
                    currentPlant.switchToVeg();
                    fillIndividualPlantView();

                }
            }
        });

        plantRecordableAdapter = new PlantRecordableTileArrayAdapter(
                getBaseContext(), R.layout.plant_recordable_tile,
                currentPlant.getAllRecordableEvents(), currentPlant);

        recordableEventListView.setAdapter(plantRecordableAdapter);
        recordableEventListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                presentRecordableEventSummaryDialog(i);
            }
        });
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START))   {
            drawer.closeDrawer(GravityCompat.START);
        }
        else if (switcher.getCurrentView() == individualPlantView)  {
            if (parentPlantViewStack.size() > 0)    {
                currentPlant = parentPlantViewStack.pop();
                fillIndividualPlantView();
            }
            else    {
                switcherToPrevious();
            }
        }
        else    {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.plant_tracker_ui, menu);
        individualPlantMenu = menu;
        individualPlantMenu.setGroupVisible(0, false);
        individualPlantMenu.setGroupVisible(1, false);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch(id)  {
            case R.id.action_rename:
                presentRenameDialog();
                break;
            case R.id.action_change_start_date:
                presentChangePlantDateDialog();
                break;
            case R.id.action_change_flower_date:
                presentChangeFlowerDateDialog();
                break;
            case R.id.action_delete_plant_really:
                tracker.deletePlant(currentPlant);
                switcherToPrevious();
                break;
            case R.id.action_archive_plant:
                currentPlant.archivePlant();
                break;
            case R.id.action_unarchive_plant:
                currentPlant.unarchivePlant();
                break;
            case R.id.action_clone_plant:
                presentAddPlantDialog(currentPlant.getPlantId());
                break;
            case R.id.add_group:
                presentAddGroupDialog();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_all_plants) {
            plantDisplay = PlantDisplay.All;
            if (switcher.getCurrentView() != allPlantsView) {
                switcherToPrevious();
            }
            else    {
                fillViewWithPlants();
            }
        }
        else if (id == R.id.nav_active_plants)  {
            plantDisplay = PlantDisplay.Active;
            if (switcher.getCurrentView() != allPlantsView) {
                switcherToPrevious();
            }
            else    {
                fillViewWithPlants();
            }
        }
        else if (id == R.id.nav_archived_plants)    {
            plantDisplay = PlantDisplay.Archived;
            if (switcher.getCurrentView() != allPlantsView) {
                switcherToPrevious();
            }
            else    {
                fillViewWithPlants();
            }
        }
        else if (id == R.id.nav_delete) {
            presentDeleteAllPlantsDialog();
        }
        else if (id == R.id.nav_add_plant)  {
            presentAddPlantDialog(0);
            MenuItem activeMenuItem = (MenuItem)toolbar.findViewById(R.id.nav_about_plant_tracker);
        }
        else if (id == R.id.nav_about_plant_tracker)    {
            presentAboutDialog();
        }
        else if (id == R.id.nav_export)   {
            if (switcher.getCurrentView() == individualPlantView)   {
                ArrayList<String> files = new ArrayList<>();
                files.add(getFilesDir() + "/plants/" + currentPlant.getPlantId() +
                        PT_FILE_EXTENSION);
                email(PlantTrackerUi.this, "", "", "Plant Tracker Export", "Plant Tracker Export",
                        files);
            }
            else    {
                ArrayList<Plant> allPlants = tracker.getAllPlants();
                ArrayList<String> files = new ArrayList<>();
                String filesDir = getFilesDir().toString();
                for(Plant p : allPlants)    {
                    files.add(filesDir + "/plants/" + p.getPlantId() + PT_FILE_EXTENSION);
                }
                email(PlantTrackerUi.this, "", "", "Plant Tracker Export", "Plant Tracker Export",
                        files);
            }
        }
        else if (id == R.id.nav_import) {
            presentImportDialog();
        }
        else    {
            if (switcher.getCurrentView() == individualPlantView)   {
                switcherToPrevious();
            }

            groupIdViewFilter = menuItemToGroupIdMapping.get(item.getItemId());
            plantDisplay = PlantDisplay.Group;
            fillViewWithPlants();
        }

        DrawerLayout drawer = (DrawerLayout)findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu)  {
        MenuItem changeFloweringDateMenuItem = (MenuItem)individualPlantMenu.findItem(
                R.id.action_change_flower_date);

        if (currentPlant == null)   {
            changeFloweringDateMenuItem.setVisible(false);
        }
        else    {
            MenuItem archivedMenuItem = individualPlantMenu.findItem(R.id.action_archive_plant);
            MenuItem activeMenuItem = individualPlantMenu.findItem(R.id.action_unarchive_plant);

            if (currentPlant.isArchived())  {
                archivedMenuItem.setVisible(false);
                activeMenuItem.setVisible(true);
            }
            else    {
                archivedMenuItem.setVisible(true);
                activeMenuItem.setVisible(false);
            }

            if(currentPlant.isFlowering())
            {
                changeFloweringDateMenuItem.setVisible(true);
            }
            else
            {
                changeFloweringDateMenuItem.setVisible(false);
            }

            // prepare add to group submenu
            if (addToGroup == null) {
                addToGroup = (SubMenu)individualPlantMenu.findItem(R.id.action_groups).getSubMenu()
                        .addSubMenu(9, 1, 1, "Add to ...");
            }

            addToGroup.clear();

            ArrayList<Group> nonMembershipGroups = tracker.getGroupsPlantIsNotMemberOf(
                    currentPlant.getPlantId());

            for(Group g : nonMembershipGroups)  {
                final Group currentGroup = g;
                MenuItem m = addToGroup.add(g.getGroupName());
                m.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        final long groupId = currentGroup.getGroupId();
                        tracker.addMemberToGroup(currentPlant.getPlantId(), groupId);
                        return true;
                    }
                });
            }

            if (nonMembershipGroups.size() == 0)    {
                individualPlantMenu.findItem(R.id.action_groups).getSubMenu()
                        .setGroupVisible(9, false);
            }
            else    {
                individualPlantMenu.findItem(R.id.action_groups).getSubMenu()
                        .setGroupVisible(9, true);
            }

            // prepare remove from group submenu
            if (removeFromGroup == null)    {
                removeFromGroup = (SubMenu)individualPlantMenu.findItem(R.id.action_groups)
                        .getSubMenu().addSubMenu(10, 2, 2, "Remove from ...");
            }

            removeFromGroup.clear();

            ArrayList<Group> membershipGroups = tracker.getGroupsPlantIsMemberOf(
                    currentPlant.getPlantId());

            for(Group mg : membershipGroups)  {
                final Group currentGroup = mg;

                MenuItem m = removeFromGroup.add(mg.getGroupName());
                m.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        final long groupId = currentGroup.getGroupId();
                        tracker.removeMemberFromGroup(currentPlant.getPlantId(), groupId);
                        return true;
                    }
                });
            }

            if (membershipGroups.size() == 0 )  {
                individualPlantMenu.findItem(R.id.action_groups).getSubMenu()
                        .setGroupVisible(10, false);
            }
            else    {
                individualPlantMenu.findItem(R.id.action_groups).getSubMenu()
                        .setGroupVisible(10, true);
            }

            // prepare rename groups menu
            MenuItem renameGroupMenuItem = individualPlantMenu.findItem(R.id.rename_group);
            SubMenu renameGroupSubMenu = renameGroupMenuItem.getSubMenu();

            renameGroupSubMenu.clear();

            for (Group g : tracker.getAllGroups()) {
                final long groupId = g.getGroupId();
                MenuItem renameMenuItem = renameGroupSubMenu.add(g.getGroupName());
                renameMenuItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        presentRenameGroupDialog(groupId);
                        refreshDrawerGroups();
                        return true;
                    }
                });
            }

            // prepare delete groups menu
            MenuItem deleteGroupItem = individualPlantMenu.findItem(R.id.delete_group);
            final SubMenu deleteGroupSubMenu = deleteGroupItem.getSubMenu();

            deleteGroupSubMenu.clear();

            for (Group g : tracker.getAllGroups())  {
                final long groupId = g.getGroupId();
                MenuItem menuItem = deleteGroupSubMenu.add(g.getGroupName());
                menuItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        tracker.removeGroup(groupId);
                        refreshDrawerGroups();
                        return true;
                    }
                });
            }
        }

        return true;
    }

    private long currentlySelectedGroup;

    private void presentRenameGroupDialog(long groupId) {
        final Dialog dialog = new Dialog(PlantTrackerUi.this);
        dialog.setContentView(R.layout.dialog_rename_group);

        final EditText groupNameEditText = (EditText)dialog.findViewById(R.id.groupNameEditText);
        final TextView groupNameTextView = (TextView)dialog.findViewById(R.id.groupNameTextView);
        groupNameTextView.setText(tracker.getGroup(groupId).getGroupName());

        final long localGroupId = groupId;

        Button okButton = (Button)dialog.findViewById(R.id.okButton);
        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (groupNameEditText.getText().toString().isEmpty())   {
                    return;
                }

                tracker.renameGroup(localGroupId, groupNameEditText.getText().toString());
                refreshDrawerGroups();
                dialog.dismiss();
            }
        });

        Button cancelButton = (Button)dialog.findViewById(R.id.cancelButton);
        cancelButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });

        try {
            dialog.show();
        }
        catch(Exception e)  {
            e.printStackTrace();
        }
    }

    private void presentImportDialog()  {
        File downloadDirectory = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS);

        final ArrayList<String> fileNames = new ArrayList<>();

        final File[] files = downloadDirectory.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                if (name.endsWith(PT_FILE_EXTENSION))    {
                    // build list of file names while we're at it...
                    fileNames.add(name);
                    return true;
                }

                return false;
            }
        });

        final Dialog dialog = new Dialog(PlantTrackerUi.this);
        dialog.setContentView(R.layout.dialog_import_plants);

        final ListView importPlantsListView = (ListView)dialog.findViewById(
                R.id.importPlantsListView);

        final ArrayList<String> selectedFiles = new ArrayList<>();

        importPlantsListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        importPlantsListView.setAdapter(new ArrayAdapter<String>(
                getBaseContext(), android.R.layout.simple_list_item_1, fileNames) {

            public void onItemClick(AdapterView<?> adapter, View arg1, int index, long arg3)    {
                if (selectedFiles.contains(fileNames.get(index)))   {
                    selectedFiles.remove(fileNames.get(index));
                }
                else    {
                    selectedFiles.add(fileNames.get(index));
                }
            }
        });

        Button okButton = (Button)dialog.findViewById(R.id.okButton);
        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ArrayList<File> fileImportCollection = new ArrayList<File>();
                for(String fileName : selectedFiles)    {
                    for(File f : files) {
                        if (f.getName().equals(fileName))   {
                            fileImportCollection.add(f);
                        }
                    }
                }

                tracker.importPlants(fileImportCollection);

                dialog.dismiss();
            }
        });

        Button cancelButton = (Button)dialog.findViewById(R.id.cancelButton);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });

        try {
            dialog.show();
        }
        catch(Exception e)  {
            e.printStackTrace();
        }
    }

    private void presentGeneralEventDialog()    {
        final Dialog dialog = new Dialog(PlantTrackerUi.this);
        dialog.setContentView(R.layout.dialog_general_event);

        Spinner groupListSpinner = (Spinner)dialog.findViewById(R.id.groupListSpinner);

        final ArrayList<Group> groups = tracker.getGroupsPlantIsMemberOf(currentPlant.getPlantId());
        final ArrayList<String> groupNames = new ArrayList<>();

        for(Group g : groups)   {
            groupNames.add(g.getGroupName());
        }

        ArrayAdapter<String> adapter = new ArrayAdapter(this,
                android.R.layout.simple_spinner_dropdown_item, groupNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        groupListSpinner.setAdapter(adapter);
        groupListSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String name = groupNames.get(position);
                for (Group g : groups)  {
                    if (g.getGroupName().equals(name))  {
                        currentlySelectedGroup = g.getGroupId();
                        break;
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                currentlySelectedGroup = 0;
            }
        });

        final CheckBox applyToGroupCheckBox = (CheckBox)dialog.findViewById(
                R.id.applyToGroupCheckbox);

        final AutoCompleteTextView generalEventName = (AutoCompleteTextView)dialog.findViewById(
                R.id.generalEventNameTextView);
        final AutoCompleteTextView generalEventNameAbbrevEditText =
                (AutoCompleteTextView)dialog.findViewById(R.id.generalEventAbbrevTextView);

        generalEventNameAbbrevEditText.setAdapter(new ArrayAdapter<String>(
                getBaseContext(), android.R.layout.simple_list_item_1,
                tracker.getPlantTrackerSettings().getAutoCompleteKeys()));

        generalEventNameAbbrevEditText.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                String selectedAbbreviation = (String)arg0.getItemAtPosition(arg2);
                if (tracker.getPlantTrackerSettings().getAutoCompleteKeys()
                        .contains(selectedAbbreviation))    {

                    generalEventName.setText(tracker.getPlantTrackerSettings()
                            .getAutoCompleteValueForKey(selectedAbbreviation));
                }
            }
        });

        generalEventNameAbbrevEditText.setThreshold(1);

        final TextView eventNotesEditText = (TextView)dialog.findViewById(
                R.id.eventNotesEditText);

        Button okButton = (Button)dialog.findViewById(R.id.okButton);
        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (generalEventName.getText().toString().equals("") ||
                        generalEventNameAbbrevEditText.getText().toString().equals("")) {
                    return;
                }

                if (applyToGroupCheckBox.isChecked() && currentlySelectedGroup > 0) {
                    ArrayList<Plant> plantMembers = tracker.getMembersOfGroup(
                            currentlySelectedGroup);

                    for (Plant p : plantMembers) {
                        p.addGeneralEvent(generalEventName.getText().toString(),
                                generalEventNameAbbrevEditText.getText().toString(),
                                eventNotesEditText.getText().toString());
                    }
                }
                else {
                    currentPlant.addGeneralEvent(generalEventName.getText().toString(),
                            generalEventNameAbbrevEditText.getText().toString(),
                            eventNotesEditText.getText().toString());
                }
                dialog.dismiss();

                if (!tracker.getPlantTrackerSettings().getAutoCompleteKeys().contains(
                        generalEventNameAbbrevEditText.getText().toString()))   {

                    tracker.getPlantTrackerSettings().addAutoCompleteKeyValuePair(
                            generalEventNameAbbrevEditText.getText().toString(),
                            generalEventName.getText().toString());
                }


                fillIndividualPlantView();
            }
        });

        Button cancelButton = (Button)dialog.findViewById(R.id.cancelButton);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });

        try {
            dialog.show();
        }
        catch(Exception e)  {
            e.printStackTrace();
        }
    }

    private void presentDeleteAllPlantsDialog() {
        final Dialog dialog = new Dialog(PlantTrackerUi.this);
        dialog.setContentView(R.layout.dialog_delete_all_plants);

        Button okButton = (Button)dialog.findViewById(R.id.okButton);
        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();

                tracker.deleteAllPlants();
                currentPlant = null;
                if (switcher.getCurrentView() == individualPlantView)  {
                    switcherToPrevious();
                }
                else    {
                    fillViewWithPlants();
                }
            }
        });

        Button cancelButton = (Button)dialog.findViewById(R.id.cancelButton);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });

        try {
            dialog.show();
        }
        catch(Exception e)  {
            e.printStackTrace();
        }
    }

    private void presentFeedingDialog() {
        final Dialog dialog = new Dialog(PlantTrackerUi.this);
        dialog.setContentView(R.layout.dialog_feed_event);
        Spinner groupListSpinner = (Spinner)dialog.findViewById(R.id.groupListSpinner);

        final ArrayList<Group> groups = tracker.getGroupsPlantIsMemberOf(currentPlant.getPlantId());
        final ArrayList<String> groupNames = new ArrayList<>();

        for(Group g : groups)   {
            groupNames.add(g.getGroupName());
        }

        ArrayAdapter<String> adapter = new ArrayAdapter(this,
                android.R.layout.simple_spinner_dropdown_item, groupNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        groupListSpinner.setAdapter(adapter);
        groupListSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String name = groupNames.get(position);
                for (Group g : groups)  {
                    if (g.getGroupName().equals(name))  {
                        currentlySelectedGroup = g.getGroupId();
                        break;
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                currentlySelectedGroup = 0;
            }
        });

        final CheckBox applyToGroupCheckBox = (CheckBox)dialog.findViewById(
                R.id.applyToGroupCheckbox);

        Button okButton = (Button)dialog.findViewById(R.id.feedOkButton);
        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                EditText pHEditText = (EditText)dialog.findViewById(R.id.feedPhEditText);
                String phInput = pHEditText.getText().toString();
                double ph = 0.0;
                try {
                    ph = Double.parseDouble(phInput);
                }
                catch (Exception e) {
                    return;
                }

                EditText feedStrengthEditText = (EditText)dialog.findViewById(
                        R.id.feedStrengthEditText);
                String feedStrengthInput = feedStrengthEditText.getText().toString();
                double str = 0.0;
                try {
                    str = Double.parseDouble(feedStrengthInput);
                }
                catch (Exception e) {
                    return;
                }

                if (applyToGroupCheckBox.isChecked() && currentlySelectedGroup > 0) {
                    ArrayList<Plant> plantMembers = tracker.getMembersOfGroup(
                            currentlySelectedGroup);

                    for (Plant p : plantMembers) {
                        p.feedPlant(str, ph);
                    }
                }
                else    {
                    currentPlant.feedPlant(str, ph);
                }

                dialog.dismiss();

                fillIndividualPlantView();
            }
        });

        Button cancelButton = (Button)dialog.findViewById(R.id.feedCancelButton);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });

        try {
            dialog.show();
        }
        catch(Exception e)  {
            e.printStackTrace();
        }
    }

    private void presentWateringDialog()    {
        final Dialog dialog = new Dialog(PlantTrackerUi.this);
        dialog.setContentView(R.layout.dialog_water_event);

        Spinner groupListSpinner = (Spinner)dialog.findViewById(R.id.groupListSpinner);

        final ArrayList<Group> groups = tracker.getGroupsPlantIsMemberOf(currentPlant.getPlantId());
        final ArrayList<String> groupNames = new ArrayList<>();

        for(Group g : groups)   {
            groupNames.add(g.getGroupName());
        }

        ArrayAdapter<String> adapter = new ArrayAdapter(this,
                android.R.layout.simple_spinner_dropdown_item, groupNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        groupListSpinner.setAdapter(adapter);
        groupListSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String name = groupNames.get(position);
                for (Group g : groups)  {
                    if (g.getGroupName().equals(name))  {
                        currentlySelectedGroup = g.getGroupId();
                        break;
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                currentlySelectedGroup = 0;
            }
        });

        final CheckBox applyToGroupCheckBox = (CheckBox)dialog.findViewById(R.id.applyToGroupCheckbox);

        Button okButton = (Button)dialog.findViewById(R.id.okButton);
        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                EditText pHEditText = (EditText)dialog.findViewById(R.id.pHEditText);
                String phInput = pHEditText.getText().toString();

                double d = 0.0;
                try {
                    d = Double.parseDouble(phInput);
                }
                catch (Exception e) {
                    return;
                }

                if (applyToGroupCheckBox.isChecked() && currentlySelectedGroup > 0)   {
                    ArrayList<Plant> plantMembers = tracker.getMembersOfGroup(
                            currentlySelectedGroup);

                    for(Plant p : plantMembers) {
                        p.waterPlant(d);
                    }

                    currentlySelectedGroup = 0;
                }
                else    {
                    currentPlant.waterPlant(d);
                }


                dialog.dismiss();

                fillIndividualPlantView();

            }
        });

        Button cancelButton = (Button)dialog.findViewById(R.id.cancelButton);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });

        try {
            dialog.show();
        }
        catch(Exception e)  {
            e.printStackTrace();
        }
    }

    private void presentAddObservationDialog()  {
        final Dialog dialog = new Dialog(PlantTrackerUi.this);
        dialog.setContentView(R.layout.dialog_observation_event);

        Spinner groupListSpinner = (Spinner)dialog.findViewById(R.id.groupListSpinner);

        final ArrayList<Group> groups = tracker.getGroupsPlantIsMemberOf(currentPlant.getPlantId());
        final ArrayList<String> groupNames = new ArrayList<>();

        for(Group g : groups)   {
            groupNames.add(g.getGroupName());
        }

        ArrayAdapter<String> adapter = new ArrayAdapter(this,
                android.R.layout.simple_spinner_dropdown_item, groupNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        groupListSpinner.setAdapter(adapter);
        groupListSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String name = groupNames.get(position);
                for (Group g : groups)  {
                    if (g.getGroupName().equals(name))  {
                        currentlySelectedGroup = g.getGroupId();
                        break;
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                currentlySelectedGroup = 0;
            }
        });

        final CheckBox applyToGroupCheckBox = (CheckBox)dialog.findViewById(
                R.id.applyToGroupCheckbox);

        Button okButton = (Button)dialog.findViewById(R.id.okButton);
        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                EditText rhMinEditText = (EditText)dialog.findViewById(R.id.rhMinEditText);
                EditText rhMaxEditText = (EditText)dialog.findViewById(R.id.rhMaxEditText);
                EditText tempMinEditText = (EditText)dialog.findViewById(R.id.tempMinEditText);
                EditText tempMaxEditText = (EditText)dialog.findViewById(R.id.tempMaxEditText);
                EditText observationsEditText = (EditText)dialog.findViewById(
                        R.id.observationNotesEditText);

                String rhMin = rhMinEditText.getText().toString();
                String rhMax = rhMaxEditText.getText().toString();
                String tempMin = tempMinEditText.getText().toString();
                String tempMax = tempMaxEditText.getText().toString();

                int minTemp = 0;
                int maxTemp = 0;
                int minRh = 0;
                int maxRh = 0;

                try {
                    minTemp = Integer.parseInt(tempMin);
                }
                catch (Exception e) {
                    return;
                }

                try {
                    maxTemp = Integer.parseInt(tempMax);
                }
                catch (Exception e) {
                    return;
                }

                try {
                    minRh = Integer.parseInt(rhMin);
                }
                catch (Exception e) {
                    return;
                }

                try {
                    maxRh = Integer.parseInt(rhMax);
                }
                catch (Exception e) {
                    return;
                }

                String observations = observationsEditText.getText().toString();

                if (applyToGroupCheckBox.isChecked() && currentlySelectedGroup > 0) {
                    ArrayList<Plant> plantMembers = tracker.getMembersOfGroup(
                            currentlySelectedGroup);

                    for(Plant p : plantMembers) {
                        p.addObservation(maxRh, minRh, maxTemp, minTemp, observations);
                    }
                }
                else    {
                    currentPlant.addObservation(maxRh, minRh, maxTemp, minTemp, observations);
                }

                dialog.dismiss();

                fillIndividualPlantView();

            }
        });

        Button cancelButton = (Button)dialog.findViewById(R.id.cancelButton);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });

        try {
            dialog.show();
        }
        catch(Exception e)  {
            e.printStackTrace();
        }
    }

    private void presentRecordableEventSummaryDialog(int eventIndex)  {
        final Dialog dialog = new Dialog(PlantTrackerUi.this);

        Recordable r = currentPlant.getAllRecordableEvents().get(eventIndex);

        TextView eventTypeTextView;
        TextView dateTextView;

        if (r instanceof EventRecord)   {
            EventRecord er = (EventRecord)r;
            dialog.setContentView(R.layout.dialog_display_recordable_event);
            eventTypeTextView = (TextView)dialog.findViewById(R.id.observEventTypeTextView);
            dateTextView = (TextView)dialog.findViewById(R.id.dateTextView);

            eventTypeTextView.setText(er.getEventString());
            dateTextView.setText(r.getTimestamp().getTime().toString());
            TextView phTextView = (TextView)dialog.findViewById(R.id.pHTextView);
            TextView foodStrengthTextView = (TextView)dialog.findViewById(
                    R.id.foodStrengthTextView);

            if (er.getEventType() == EventRecord.PlantEvent.Food ||
                    er.getEventType() == EventRecord.PlantEvent.Water)  {
                phTextView.setText(""+er.getpH());
                foodStrengthTextView.setText(""+er.getFoodStrength());
                RelativeLayout phLayout = (RelativeLayout)dialog.findViewById(
                        R.id.waterLayout);
                phLayout.setVisibility(View.VISIBLE);
            }
            else    {
                RelativeLayout phLayout = (RelativeLayout)dialog.findViewById(
                        R.id.waterLayout);
                phLayout.setVisibility(View.INVISIBLE);
            }

        }
        else if(r instanceof ObservationRecord) {
            ObservationRecord or = (ObservationRecord)r;
            dialog.setContentView(R.layout.dialog_display_observation);
            eventTypeTextView = (TextView)dialog.findViewById(R.id.observEventTypeTextView);
            dateTextView = (TextView)dialog.findViewById(R.id.observDateTextView);
            TextView tempTextView = (TextView)dialog.findViewById(R.id.tempTextView);
            TextView rhTextView = (TextView)dialog.findViewById(R.id.rhTextView);
            TextView notesTextView = (TextView)dialog.findViewById(R.id.notesTextView);

            eventTypeTextView.setText("Observation");
            dateTextView.setText(r.getTimestamp().getTime().toString());
            tempTextView.setText(or.getTempLow() + "/" + or.getTempHigh());
            rhTextView.setText(or.getRhLow() + "/" + or.getRhHigh());
            notesTextView.setText(or.getNotes());
        }

        try {
            dialog.show();
        }
        catch(Exception e)  {
            e.printStackTrace();
        }
    }

    private void presentRenameDialog()  {
        final Dialog dialog = new Dialog(PlantTrackerUi.this);
        dialog.setContentView(R.layout.dialog_rename);
        final EditText renameEditText = (EditText)dialog.findViewById(R.id.renameEditText);
        renameEditText.setText(currentPlant.getPlantName());

        Button okButton = (Button)dialog.findViewById(R.id.okButton);
        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                currentPlant.setPlantName(renameEditText.getText().toString());

                dialog.dismiss();

                fillIndividualPlantView();

            }
        });

        Button cancelButton = (Button)dialog.findViewById(R.id.cancelButton);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });

        try {
            dialog.show();
        }
        catch(Exception e)  {
            e.printStackTrace();
        }
    }

    private void presentAddPlantDialog(final long parentPlantId)    {
        final Dialog dialog = new Dialog(PlantTrackerUi.this);
        dialog.setContentView(R.layout.dialog_new_plant);

        Button okButton = (Button)dialog.findViewById(R.id.okButton);
        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                EditText plantNameEditText = (EditText)dialog.findViewById(
                        R.id.plantNameEditText);

                String plantName = plantNameEditText.getText().toString();

                RadioGroup rg = (RadioGroup)dialog.findViewById(R.id.originRadioGroup);
                boolean isFromSeed;
                int selectedId = rg.getCheckedRadioButtonId();
                RadioButton selectedOrigin =(RadioButton)dialog.findViewById(selectedId);
                RadioButton cloneRadioButton = (RadioButton)dialog.findViewById(
                        R.id.cloneRadioButton);

                if (parentPlantId > 0)   {
                    isFromSeed = false;
                }
                else    {
                    if (selectedOrigin == cloneRadioButton &&
                            selectedOrigin.isChecked())    {
                        isFromSeed = false;
                    }
                    else    {
                        isFromSeed = true;
                    }
                }

                Calendar c = Calendar.getInstance();

                DatePicker datePicker = (DatePicker)dialog.findViewById(R.id.datePicker);
                int year = datePicker.getYear();
                int month = datePicker.getMonth();
                int dayOfMonth = datePicker.getDayOfMonth();

                c.set(year, month, dayOfMonth);

                if (parentPlantId > 0)  {
                    tracker.addPlant(c, plantName, parentPlantId);
                }
                else    {
                    tracker.addPlant(c, plantName, isFromSeed);
                }

                dialog.dismiss();

                fillViewWithPlants();
            }
        });

        Button cancelButton = (Button)dialog.findViewById(R.id.cancelButton);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });

        try {
            dialog.show();
        }
        catch(Exception e)  {
            e.printStackTrace();
        }
    }

    private void presentChangePlantDateDialog() {
        final Dialog dialog = new Dialog(PlantTrackerUi.this);
        dialog.setContentView(R.layout.dialog_change_date);

        Button okButton = (Button)dialog.findViewById(R.id.okButton);
        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DatePicker dateChangePicker = (DatePicker)dialog.findViewById(
                        R.id.dateChangePicker);

                Calendar c = Calendar.getInstance();
                c.set(dateChangePicker.getYear(), dateChangePicker.getMonth(),
                        dateChangePicker.getDayOfMonth());

                currentPlant.changePlantingDate(c);

                dialog.dismiss();

                fillIndividualPlantView();

            }
        });

        Button cancelButton = (Button)dialog.findViewById(R.id.cancelButton);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });

        try {
            dialog.show();
        }
        catch(Exception e)  {
            e.printStackTrace();
        }
    }

    private void presentChangeFlowerDateDialog()    {
        final Dialog dialog = new Dialog(PlantTrackerUi.this);
        dialog.setContentView(R.layout.dialog_change_date);

        Button okButton = (Button)dialog.findViewById(R.id.okButton);
        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DatePicker dateChangePicker = (DatePicker)dialog.findViewById(
                        R.id.dateChangePicker);

                Calendar c = Calendar.getInstance();
                c.set(dateChangePicker.getYear(), dateChangePicker.getMonth(),
                        dateChangePicker.getDayOfMonth());

                currentPlant.changeFloweringDate(c);

                dialog.dismiss();

                fillIndividualPlantView();

            }
        });

        Button cancelButton = (Button)dialog.findViewById(R.id.cancelButton);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });

        try {
            dialog.show();
        }
        catch(Exception e)  {
            e.printStackTrace();
        }
    }

    private void presentAboutDialog()   {
        final Dialog dialog = new Dialog(PlantTrackerUi.this);
        dialog.setContentView(R.layout.dialog_about);

        dialog.show();
    }

    private void presentAddGroupDialog()    {
        final Dialog dialog = new Dialog(PlantTrackerUi.this);
        dialog.setContentView(R.layout.dialog_add_group);

        final EditText groupNameEditText = (EditText)dialog.findViewById(R.id.groupNameEditText);
        Button okButton = (Button)dialog.findViewById(R.id.okButton);
        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (groupNameEditText.getText().toString().isEmpty())   {
                    return;
                }

                tracker.addGroup(groupNameEditText.getText().toString());
                tracker.savePlant(currentPlant);
                refreshDrawerGroups();
                dialog.dismiss();
            }
        });

        Button cancelButton = (Button)dialog.findViewById(R.id.cancelButton);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    private void refreshDrawerGroups()   {
        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        int count = 400;
        Menu drawerMenu = navigationView.getMenu();
        MenuItem sm = drawerMenu.findItem(R.id.viewsMenuItem);
        sm.getSubMenu().removeGroup(334);

        ArrayList<Group> allGroups = tracker.getAllGroups();
        for(Group g : allGroups)    {
            MenuItem groupMenuItem = sm.getSubMenu().add(334, count, count, "Group: " +
                    g.getGroupName());
            groupMenuItem.setIcon(R.drawable.ic_group_plants);
            menuItemToGroupIdMapping.put(groupMenuItem.getItemId(), g.getGroupId());
            count++;
        }
    }

    // switch to all plants
    private void switcherToPrevious()    {
        parentPlantViewStack.clear();   // clear the view stack
        switcher.showPrevious();
        individualPlantMenu.setGroupVisible(0, false);
        individualPlantMenu.setGroupVisible(1, false);
        fillViewWithPlants();
    }

    // switch to individual plant
    private void switcherToNext()   {
        switcher.showNext();
        individualPlantMenu.setGroupVisible(0, true);
        individualPlantMenu.setGroupVisible(1, true);
    }

    private String formatDate(Calendar c)   {
        SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy");
        return sdf.format(c.getTime());
    }

    // email files
    public void email(Context context, String emailTo, String emailCC,
                             String subject, String emailText, List<String> filePaths) {

        //need to "send multiple" to get more than one attachment
        final Intent emailIntent = new Intent(Intent.ACTION_SEND_MULTIPLE);

        emailIntent.setType("text/plain");
        emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL, new String[]{emailTo});
        emailIntent.putExtra(android.content.Intent.EXTRA_CC, new String[]{emailCC});
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
        emailIntent.putExtra(Intent.EXTRA_TEXT, emailText);

        //has to be an ArrayList
        ArrayList<Uri> uris = new ArrayList<Uri>();

        //convert from paths to Android friendly Parcelable Uri's
        for (String file : filePaths) {
            File fileIn = new File(file);
            Uri u = FileProvider.getUriForFile(context, "com.nonsense.planttracker.provider",
                    fileIn);

            uris.add(u);
        }

        emailIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
        context.startActivity(Intent.createChooser(emailIntent, "Send mail..."));
    }
}