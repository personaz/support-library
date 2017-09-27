package id.my.personne.support;

import java.net.ProtocolException;

import id.my.personne.library.AsyncRequest;

/**
 * Created by surya on 9/27/17.
 */

public class AsyncChildRequest extends AsyncRequest {

    public AsyncChildRequest(String url, RequestFinishedListener finishedListener) {
        super(url, finishedListener);
    }

    @Override
    protected byte[] doInBackground(Void... voids) {
        try {
            connection.setRequestMethod(GET_METHOD);
        } catch (ProtocolException e) {
            e.printStackTrace();
        }
        return super.doInBackground(voids);
    }
}
