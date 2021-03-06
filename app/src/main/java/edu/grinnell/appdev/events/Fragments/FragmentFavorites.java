package edu.grinnell.appdev.events.Fragments;


import android.app.Activity;
import android.app.Notification;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;

import edu.grinnell.appdev.events.Adapters.FavoritesRecyclerViewAdapter;
import edu.grinnell.appdev.events.Adapters.HomeRecyclerViewAdapter;
import edu.grinnell.appdev.events.Model.Event;
import edu.grinnell.appdev.events.NotificationHandler.NotificationHandler;
import edu.grinnell.appdev.events.R;
import edu.grinnell.appdev.events.Misc.RecyclerItemTouchHelper;

import static android.support.v7.widget.DividerItemDecoration.VERTICAL;
import static edu.grinnell.appdev.events.Misc.Constants.FAVORITES_LIST;
import static edu.grinnell.appdev.events.MainActivity.favoritesList;
import static edu.grinnell.appdev.events.MainActivity.writeToFile;


/**
 * A simple {@link Fragment} subclass.
 */
public class FragmentFavorites extends Fragment implements RecyclerItemTouchHelper.RecyclerItemTouchHelperListener {

    private RecyclerView recyclerView;
    private TextView emptyView;
    private FavoritesRecyclerViewAdapter recyclerViewAdapter;
    private CoordinatorLayout coordinatorLayout;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_favorites, container, false);
    }

    /**
     * Set up the recycler view for favorites items
     * @param activity Activity in which the recycler view resides
     */
    private void configureRecyclerView(Activity activity){

        recyclerView.hasFixedSize();
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(activity);
        recyclerView.setLayoutManager(layoutManager);
        recyclerViewAdapter = new FavoritesRecyclerViewAdapter();
        recyclerView.addItemDecoration(new DividerItemDecoration(getActivity(), VERTICAL));
        recyclerView.setAdapter(recyclerViewAdapter);

        ItemTouchHelper.SimpleCallback itemTouchHelperCallback = new RecyclerItemTouchHelper(0, ItemTouchHelper.LEFT, this);
        new ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(recyclerView);


    }

    /**
     *
     * @param id Unique id of an event, which need to be delete
     * @param favorites List of events from which the event is deleted
     */
    public static void removeWithID(String id, ArrayList<Event> favorites){
        for (int i =0; i < favorites.size(); i++) {
            if (favorites.get(i).getId().equals(id)) {
                favorites.remove(i);
            }
        }
    }

    /**
     *
     * @param event Event object to be added
     * @param favorites List in which event object is added
     */
    public static void addEvent (Event event, ArrayList<Event> favorites, int position){
        for (Event e: favorites){
            if (e.getId().equals(event.getId())){
                return;
            }
        }
        if (position < 0) {
            favorites.add(0, event);
        }
        else {
            favorites.add(position, event);
        }
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = getView().findViewById(R.id.favorites_recycler);
        emptyView = getView().findViewById(R.id.empty_view);

        //Decide which view to show depending on if the list is empty or not
        if (!favoritesList.isEmpty()) {
            configureRecyclerView(getActivity());
            emptyView.setVisibility(View.GONE);
            recyclerView.setVisibility(view.VISIBLE);
        }
        else {
            emptyView.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        }
        coordinatorLayout = getView().findViewById(R.id.frag_fav_cl);
    }

    /**
     * Reset recycler view after an item has been deleted with a swipe
     * @param viewHolder
     * @param direction
     * @param position
     */
    @Override
    public void onSwiped(final RecyclerView.ViewHolder viewHolder, int direction, final int position) {
        Event itemRemoved = favoritesList.get(position);
        removeWithID(itemRemoved.getId(), favoritesList);
        writeToFile(getContext(), FAVORITES_LIST, favoritesList);
        recyclerViewAdapter.notifyItemRemoved(position);

        int eventId = HomeRecyclerViewAdapter.getIdInt(itemRemoved.getId());
        NotificationHandler.deleteNotification((Activity) getContext(), eventId);

        displaySnackBarWithBottomMargin(position, itemRemoved, eventId);

        if(favoritesList.isEmpty()){
            emptyView.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        }
    }

    /**
     *
     * @param position Position to insert the deleted item
     * @param event Event to be added back
     */
    private void displaySnackBarWithBottomMargin(final int position, final Event event, final int eventId) {

        // Get the margin for snackbar, which is equal to the height of the bottom navigation view
        TypedValue tv = new TypedValue();
        int actionBarHeight = 0;
        if (getActivity().getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true))
        {
            actionBarHeight = TypedValue.complexToDimensionPixelSize(tv.data,getResources().getDisplayMetrics());
        }
        Snackbar snackbar = Snackbar.make(coordinatorLayout, "Event removed from the List",Snackbar.LENGTH_SHORT);
        final View snackBarView = snackbar.getView();
        final CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) snackBarView.getLayoutParams();

        // Set the margin
        params.setMargins(params.leftMargin,
                params.topMargin,
                params.rightMargin,
                params.bottomMargin + actionBarHeight);

        snackBarView.setLayoutParams(params);
        snackbar.setAction("UNDO!", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addEvent(event, favoritesList, position);
                if (favoritesList.size() == 1){
                    getView();
                    recyclerView.setVisibility(View.VISIBLE);
                    emptyView.setVisibility(View.GONE);
                }
                recyclerViewAdapter.notifyItemInserted(position);
                writeToFile(getContext(), FAVORITES_LIST, favoritesList);
                Notification notification = NotificationHandler.buildNotfication(getActivity(), event.getTitle(), "Event has started");
                NotificationHandler.createNotification(getActivity(), eventId, event.getStartTimeNew(), notification);

            }
        });
        snackbar.show();
    }
}
