package id.my.personne.support;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import org.json.JSONException;

import java.io.IOException;

import id.my.personne.library.AsyncRequest;
import id.my.personne.library.Crypt;
import id.my.personne.library.JSONObjection;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    protected void onResume() {
        super.onResume();

        String text = "Secret text";
        String password = "SecretPassword";
        try {
            String encryptText = Crypt.encryptString(password, text);
            Log.i("Result", encryptText); // return encrypted text
            String decryptText = Crypt.decryptString(password, encryptText);
            Log.i("Result", decryptText); // return "Secret text"
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            JSONObjection fromAsset = JSONObjection.assetToJsonObject(getApplicationContext(), "sample.json");
            if (fromAsset != null) {
                int secondVal = fromAsset.getInteger("key_b", 9);
                Log.i("second", String.valueOf(secondVal)); // return 9
                String value = fromAsset.getString("key_d", "empty");
                Log.i("null value", value); // return "empty"
            }
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }

        AsyncRequest request = new AsyncRequest("url_request", new AsyncRequest.RequestFinishedListener() {
            @Override
            public void onFinished(byte[] result) {
                String response = new String(result);
                Log.i("response", response);
            }
        });

        request.setMethod(AsyncRequest.GET_METHOD)
                .setMoreProgress(new AsyncRequest.InsertBackgroundProgress() {
                    @Override
                    public void onBackgroundSuccess(byte[] data) {
                        // background process
                    }
                })
                .setBodyForm("field_text", "values")
                .setBodyForm("field_int", 10)
                .execute();
    }
}
