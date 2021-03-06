package com.filippoints.activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.filippoints.R;
import com.filippoints.controller.Controller;
import com.filippoints.model.Person;
import com.filippoints.util.InternetCheck;
import com.filippoints.util.Util;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Created by hlib on 6/19/18.
 */

public class ChoosePersonActivity extends AppCompatActivity {
    private static final String POINTS_STRING = "POINTS";
    private static final String WEB_APP_URL = "http://www.filippoints.com/all/";

    private static final String PERSONS_PREFS_NAME = "persons_pref";
    private static final String PERSONS_KEY = "persons_key";
    private static final String IS_ADMIN_STRING = "IS_ADMIN";

    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private int points;
    private boolean isAdmin;

    public static Intent buildIntent(Context context, int points) {
        Intent intent = new Intent(context, ChoosePersonActivity.class);
        intent.putExtra(IS_ADMIN_STRING, true);
        intent.putExtra(POINTS_STRING, points);
        return intent;
    };

    public static Intent buildIntent(Context context) {
        Intent intent = new Intent(context, ChoosePersonActivity.class);
        intent.putExtra(IS_ADMIN_STRING, false);
        return intent;
    };

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        points = getIntent().getIntExtra(POINTS_STRING, -1);
        isAdmin = getIntent().getBooleanExtra(IS_ADMIN_STRING, false);
        if (isAdmin && points == -1){
            throw new RuntimeException("Points not specified!");
        }
        setContentView(R.layout.activity_choose_person);

        Button goToWebAppButton = findViewById(R.id.go_to_web_app_button);
        goToWebAppButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(WEB_APP_URL));
                startActivity(browserIntent);
            }
        });
        recyclerView = findViewById(R.id.recycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(isAdmin ? new AdminAdapter() : new Adapter());

        swipeRefreshLayout = findViewById(R.id.swiperefresh);
        swipeRefreshLayout.setOnRefreshListener(this::updatePeopleFromPreferencesIfOnline);

        updatePeopleFromPreferences();
        updatePeopleFromBackend();
    }

    private void updatePeopleFromPreferencesIfOnline() {
        new InternetCheck(online -> {
            if (!online) {
                noConnection();
            } else {
                updatePeopleFromPreferences();
            }
        });
    }

    private void updatePeopleFromPreferences() {
        SharedPreferences prefs = getSharedPreferences(PERSONS_PREFS_NAME, MODE_PRIVATE);
        String personsJson = prefs.getString(PERSONS_KEY, "");
        List<Person> personList = new Gson().fromJson(personsJson, new TypeToken<List<Person>>(){}.getType());
        personList = personList != null ? personList: new ArrayList<>();
        ((Adapter)recyclerView.getAdapter()).setValues(personList);
        swipeRefreshLayout.setRefreshing(false);
    }

    private void updatePeopleFromBackend() {
        Call<List<Person>> call = Controller.getWebAPI(this).getPeople(5);
        call.enqueue(new Callback<List<Person>>() {
            @Override
            public void onResponse(Call<List<Person>> call, Response<List<Person>> response) {
                if (response.isSuccessful()) {
                    List<Person> persons = response.body();
                    SharedPreferences.Editor prefsEditor
                            = getSharedPreferences(PERSONS_PREFS_NAME, MODE_PRIVATE).edit();
                    String personJson = new Gson().toJson(persons);
                    prefsEditor.putString(PERSONS_KEY, personJson);
                    prefsEditor.apply();
                }
            }

            @Override
            public void onFailure(Call<List<Person>> call, Throwable t) {
                /* Do nothing! */
            }
        });
    }

    private void noConnection() {
        Util.noConnectionSnackbar(findViewById(android.R.id.content));
        swipeRefreshLayout.setRefreshing(false);
    }

    private class PersonViewHolder extends RecyclerView.ViewHolder {

        private Person person;

        public Person getPerson() {
            return person;
        }

        public void setPerson(Person person) {
            this.person = person;
        }

        public PersonViewHolder(View itemView) {
            super(itemView);
        }
    }

    private class AdminAdapter extends Adapter {
        protected void onClick(int position) {
            int personId = values.get(position).getPk();
            Intent intent = PointsReasonActivity.buildIntent(ChoosePersonActivity.this, personId, ChoosePersonActivity.this.points);
            startActivity(intent);
        }
    }

    private class Adapter extends RecyclerView.Adapter<PersonViewHolder> {

        protected List<Person> values = new ArrayList<>();

        Adapter(List<Person> values) {
            this.values = values;
        }

        public Adapter() {
        }

        protected void onClick(int position) {
            /* Do nothing */
        }

        public void setValues(List<Person> values) {
            this.values = values;
        }

        @Override
        public PersonViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_view, parent, false);
            return new PersonViewHolder(view);
        }

        @Override
        public void onBindViewHolder(PersonViewHolder holder, final int position) {
            Person person = values.get(position);
            ((TextView)holder.itemView.findViewById(R.id.person_list_item))
                    .setText(String.format("%s %s", person.getFirst_name(), person.getLast_name()));
            ((TextView)holder.itemView.findViewById(R.id.person_points))
                    .setText(String.format("%d", person.getPoints()));
            holder.itemView.setOnClickListener(view -> {onClick(position);});
        }

        @Override
        public int getItemCount() {
            return values.size();
        }
    }
}

