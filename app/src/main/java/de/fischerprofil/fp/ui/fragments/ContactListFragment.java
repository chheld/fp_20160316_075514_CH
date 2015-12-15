package de.fischerprofil.fp.ui.fragments;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import de.fischerprofil.fp.AppController;
import de.fischerprofil.fp.R;
import de.fischerprofil.fp.model.contact.Kontakt;
import de.fischerprofil.fp.model.contact.Kontaktliste;
import de.fischerprofil.fp.rest.HttpsJsonObjectRequest;
import de.fischerprofil.fp.rest.HttpsTrustManager;
import de.fischerprofil.fp.rest.RestUtils;
import de.fischerprofil.fp.ui.ContactListActivity;
import de.fischerprofil.fp.ui.UIUtils;
import de.fischerprofil.fp.ui.adapter.ContactListAdapter;


@SuppressLint("ValidFragment")
public class ContactListFragment extends Fragment {

    private AppController mAppController;
    private Context mContext;
    private Kontaktliste mListe = new Kontaktliste();
    private ContactListAdapter mAdapter = null;
    private int mSearchRequestCounter = 0;      // Zaehler fuer die http-Anfragen
    private String mSearchString;
    private ListView mListView;
    private ProgressBar mProgressBar;
    private final String VOLLEY_TAG = "VOLLEY_TAG_ContactListFragment";
    private final String URL = RestUtils.getURL();

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (savedInstanceState != null) {
            //TODO: Restore the fragment's state here
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        //TODO: Save the fragment's state here
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        mContext = getActivity();
        mAppController = AppController.getInstance();

        View view = inflater.inflate(R.layout.fragment_contactlist, container, false);
        mListView = (ListView) view.findViewById(R.id.listview);
        mProgressBar = (ProgressBar) view.findViewById(R.id.progressBar);

        mSearchString = getArguments().getString("search", null); // evtl. übergebene SUCH-Parameter ermitteln
        if (mSearchString != null) doSearch(mSearchString);

        return view;
    }

    @Override
    public void onStop() {
        super.onStop();
        // This will tell to Volley to cancel all the pending requests
        mAppController.cancelPendingRequests(VOLLEY_TAG);
    }

    private void doSearch(String search) {

        if (search.length() < 1) {
            Toast.makeText(mContext, "Mindestens 2 Zeichen eingeben", Toast.LENGTH_SHORT).show();
        } else {
            UIUtils.makeToast(mContext, "Suche '" + search + "'");

            //Adapter erzeugen und Listview zuweisen
            mAdapter = new ContactListAdapter(mContext, mListe.getList());
            mListView.setAdapter(mAdapter);

            mProgressBar.setVisibility(View.VISIBLE);  // Fortschritts-Anzeige sichtbar

            // Bei Auswahl eines Listeneintrags neue Activity starten
            mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                    mProgressBar.setVisibility(View.GONE);  // Fortschrittsanzeige ausblenden

                    Kontakt kontakt = (Kontakt) parent.getItemAtPosition(position);
                    UIUtils.makeToast(mContext,kontakt.getPERSONNR());

                    //TODO: speichern des Auftrags in letzte Vorgänge

                    Intent intent = new Intent(mContext, ContactListActivity.class);
                    intent.putExtra("kontakt", kontakt);
                    startActivity(intent);
                }
            });
            // start http requests
            mSearchRequestCounter = 0;
            callAPIContactsByPersonNr(URL + "/contacts?relperson__personnr=" + RestUtils.cleanURL(search) + "%25"); // '%' = %25
            callAPIContactsByFirmaNr(URL + "/contacts?relfirma__firmanr=" + RestUtils.cleanURL(search) + "%25"); // '%' = %25
            callAPIContactsByFirmaKTxt(URL + "/contacts?relfirma__ktxt=" + RestUtils.cleanURL(search) + "%25"); // '%' = %25
            callAPIContactsByName(URL + "/contacts?relperson__name=" + RestUtils.cleanURL(search) + "%25"); // '%' = %25
            callAPIContactsByName(URL + "/contacts?relperson__vorname=" + RestUtils.cleanURL(search) + "%25"); // '%' = %25
        }
    }

    private void callAPIContactsByPersonNr(String search) {

        // Increase counter for pending search requests
        mSearchRequestCounter++;

        HttpsTrustManager.allowAllSSL();  // SSL-Fehlermeldungen ignorieren

        HttpsJsonObjectRequest req = new HttpsJsonObjectRequest(search, new Response.Listener<JSONObject>() {

            @Override
            public void onResponse(JSONObject response) {
                try {
                    VolleyLog.v("Response:%n %s", response.toString(4));
                    JSONArray contacts = response.getJSONArray("contacts");
                    mListe.add(contacts);
                    mAdapter.notifyDataSetChanged();
                    mSearchRequestCounter--;
                    if (mSearchRequestCounter < 1) {
                        mProgressBar.setVisibility(View.GONE);  // Fortschritt ausblenden
                        Toast.makeText(mContext, contacts.length() + " Einträge über PERSONNR gefunden", Toast.LENGTH_SHORT).show();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    mProgressBar.setVisibility(View.GONE);  // Fortschritt ausblenden
                }
            }
        }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                VolleyLog.e("Error: ", error.getMessage());
                Toast.makeText(mContext, error.toString(), Toast.LENGTH_SHORT).show();
                mSearchRequestCounter--;
                if (mSearchRequestCounter < 1) mProgressBar.setVisibility(View.GONE);  // Fortschritt ausblenden
            }
        });
        req.setRetryPolicy(new DefaultRetryPolicy(3000, 3, 2));
        AppController.getInstance().addToRequestQueue(req,VOLLEY_TAG);
    }

    private void callAPIContactsByFirmaNr(String search) {

        // Increase counter for pending search requests
        mSearchRequestCounter++;

        HttpsTrustManager.allowAllSSL();  // SSL-Fehlermeldungen ignorieren

        HttpsJsonObjectRequest req = new HttpsJsonObjectRequest(search, new Response.Listener<JSONObject>() {

            @Override
            public void onResponse(JSONObject response) {
                try {
                    VolleyLog.v("Response:%n %s", response.toString(4));
                    JSONArray contacts = response.getJSONArray("contacts");
                    mListe.add(contacts);
                    mAdapter.notifyDataSetChanged();
                    mSearchRequestCounter--;
                    if (mSearchRequestCounter < 1) {
                        mProgressBar.setVisibility(View.GONE);  // Fortschritt ausblenden
                        Toast.makeText(mContext, contacts.length() + " Einträge über FIRMANR gefunden", Toast.LENGTH_SHORT).show();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    mProgressBar.setVisibility(View.GONE);  // Fortschritt ausblenden
                }
            }
        }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                VolleyLog.e("Error: ", error.getMessage());
                Toast.makeText(mContext, error.toString(), Toast.LENGTH_SHORT).show();
                mSearchRequestCounter--;
                if (mSearchRequestCounter < 1) mProgressBar.setVisibility(View.GONE);  // Fortschritt ausblenden
            }
        });
        req.setRetryPolicy(new DefaultRetryPolicy(3000, 3, 2));
        AppController.getInstance().addToRequestQueue(req,VOLLEY_TAG);
    }

    private void callAPIContactsByFirmaKTxt(String search) {

        // Increase counter for pending search requests
        mSearchRequestCounter++;

        HttpsTrustManager.allowAllSSL();  // SSL-Fehlermeldungen ignorieren

        HttpsJsonObjectRequest req = new HttpsJsonObjectRequest(search, new Response.Listener<JSONObject>() {

            @Override
            public void onResponse(JSONObject response) {
                try {
                    VolleyLog.v("Response:%n %s", response.toString(4));
                    JSONArray contacts = response.getJSONArray("contacts");
                    mListe.add(contacts);
                    mAdapter.notifyDataSetChanged();
                    mSearchRequestCounter--;
                    if (mSearchRequestCounter < 1) {
                        mProgressBar.setVisibility(View.GONE);  // Fortschritt ausblenden
                        Toast.makeText(mContext, contacts.length() + " Einträge über FIRMA_KTXT gefunden", Toast.LENGTH_SHORT).show();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    mProgressBar.setVisibility(View.GONE);  // Fortschritt ausblenden
                }
            }
        }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                VolleyLog.e("Error: ", error.getMessage());
                Toast.makeText(mContext, error.toString(), Toast.LENGTH_SHORT).show();
                mSearchRequestCounter--;
                if (mSearchRequestCounter < 1) mProgressBar.setVisibility(View.GONE);  // Fortschritt ausblenden
            }
        });
        req.setRetryPolicy(new DefaultRetryPolicy(3000, 3, 2));
        AppController.getInstance().addToRequestQueue(req,VOLLEY_TAG);
    }

    private void callAPIContactsByName(String search) {

        // Increase counter for pending search requests
        mSearchRequestCounter++;

        HttpsTrustManager.allowAllSSL();  // SSL-Fehlermeldungen ignorieren

        HttpsJsonObjectRequest req = new HttpsJsonObjectRequest(search, new Response.Listener<JSONObject>() {

            @Override
            public void onResponse(JSONObject response) {
                try {
                    VolleyLog.v("Response:%n %s", response.toString(4));
                    JSONArray contacts = response.getJSONArray("contacts");
                    mListe.add(contacts);
                    mAdapter.notifyDataSetChanged();
                    mSearchRequestCounter--;
                    if (mSearchRequestCounter < 1) {
                        mProgressBar.setVisibility(View.GONE);  // Fortschritt ausblenden
                        Toast.makeText(mContext, contacts.length() + " Einträge zu NAME gefunden", Toast.LENGTH_SHORT).show();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    mProgressBar.setVisibility(View.GONE);  // Fortschritt ausblenden
                }
            }
        }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                VolleyLog.e("Error: ", error.getMessage());
                Toast.makeText(mContext, error.toString(), Toast.LENGTH_SHORT).show();
                mSearchRequestCounter--;
                if (mSearchRequestCounter < 1) mProgressBar.setVisibility(View.GONE);  // Fortschritt ausblenden
            }
        });
        req.setRetryPolicy(new DefaultRetryPolicy(3000, 3, 2));
        AppController.getInstance().addToRequestQueue(req,VOLLEY_TAG);
    }
}


