package id.my.personne.library;

import android.content.Context;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

/**
 * Created by surya on 9/16/17.
 */

public class JSONObjection extends JSONObject {
    public JSONObjection() {
    }

    public JSONObjection(Map copyFrom) {
        super(copyFrom);
    }

    public JSONObjection(JSONTokener readFrom) throws JSONException {
        super(readFrom);
    }

    public JSONObjection(String json) throws JSONException {
        super(json);
    }

    public JSONObjection(JSONObject copyFrom, String[] names) throws JSONException {
        super(copyFrom, names);
    }

    public static JSONObjection assetToJsonObject(Context context, String filename) throws IOException, JSONException {
        InputStream inputStream = context.getAssets().open(filename);
        byte[] buffer = new byte[inputStream.available()];
        String jsonString = null;
        if (inputStream.read(buffer) > 0) {
            jsonString = new String(buffer);
        }
        inputStream.close();
        if (jsonString != null) {
            return new JSONObjection(jsonString);
        }
        return null;
    }

    public Integer getInteger(String name, Integer defaultValue) throws JSONException {
        if (this.has(name) && !this.isNull(name)) {
            return this.getInt(name);
        }
        return defaultValue;
    }

    public String getString(String name, String defaultValue) throws JSONException {
        if (this.has(name) && !this.isNull(name)) {
            return this.getString(name);
        }
        return defaultValue;
    }

    public Double getDouble(String name, Double defaultValue) throws JSONException {
        if (this.has(name) && !this.isNull(name)) {
            return this.getDouble(name);
        }
        return defaultValue;
    }

    public Long getLong(String name, Long defaultValue) throws JSONException {
        if (this.has(name) && !this.isNull(name)) {
            return this.getLong(name);
        }
        return defaultValue;
    }
}
