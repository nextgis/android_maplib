package com.nextgis.maplib.datasource.ngw;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class CollectorResource extends Resource {

    List<LayerWithStyles> layers= new  ArrayList<>();

    public CollectorResource(JSONObject json, Connection connection) {
        super(json, connection);


        mConnection = connection;
        try {
            JSONObject projects = json.getJSONObject("collector_project");
            JSONObject rootItem = projects.getJSONObject("root_item");
            JSONArray childrenArray = rootItem.getJSONArray("children");

            for (int i = 0; i < childrenArray.length(); i++) {



                JSONObject jsonResource = childrenArray.getJSONObject(i);
                LayerWithStyles newLayer = new LayerWithStyles(jsonResource,connection );
                layers.add(newLayer);

//                mHasChildren = jsonResource.getBoolean("children");
//                if (jsonResource.has("description")) {
//                    mDescription = jsonResource.getString("description");
//                }
//
//                mName = jsonResource.getString("display_name");
//                mRemoteId = jsonResource.getLong("id");
//                mType = mConnection.getType(jsonResource.getString("cls"));
//
//                if (jsonResource.has("keyname")) {
//                    mKeyName = jsonResource.getString("keyname");
//                }
//                if (jsonResource.has("owner_user")) {
//                    JSONObject jsonObjectOwnerUser = jsonResource.getJSONObject("owner_user");
//                    if (jsonObjectOwnerUser.has("id") && !jsonObjectOwnerUser.isNull("id")) {
//                        mOwnerId = jsonObjectOwnerUser.getLong("id");
//                    }
//                }

            }
            } catch(JSONException e){
                e.printStackTrace();
            }

        mId = Connections.getNewId();



    }

    @Override
    public int getChildrenCount() {
        return 0;
    }

    @Override
    public INGWResource getChild(int i) {
        return null;
    }
}
