package id.my.personne.library;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.View;
import android.webkit.MimeTypeMap;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Created by surya on 9/16/17.
 */

public class AsyncRequest extends AsyncTask<Void, Long, byte[]> {

    public static final String POST_METHOD = "POST";
    public static final String GET_METHOD = "GET";
    public static final String PATCH_METHOD = "PATCH";
    public static final String PUT_METHOD = "PUT";
    public static final String DELETE_METHOD = "DELETE";
    public static final String CONTENT_TYPE_JSON = "application/json";
    public static final String CONTENT_TYPE_FORM = "application/x-www-form-urlencoded";
    public static final String CONTENT_TYPE_MULTIPART = "multipart/form-data";
    public static final String HEADER_CONTENT_TYPE = "Content-Type";
    public static final String HEADER_ACCEPT = "Accept";
    public static final String HEADER_AUTH = "Authorization";
    public static final String AUTH_BASIC = "Basic";
    public static final String AUTH_BEARER = "Bearer";
    private static final int DEFAULT_BUFFER_SIZE = 1024;
    private String requestUrl;
    private HttpURLConnection connection;
    private String requestMethod;
    private List<KeyValuePair> requestProperty;
    private int requestTimeout;
    private String rawBodyString;
    private byte[] rawBodyBytes;
    private List<KeyValuePair> bodyForm;
    private Activity loadingContainer;
    private static final String DOUBLE_DASH = "--";
    private static final String BREAK = "\r\n";
    private static final String BOUNDARY = "boundary";
    private static final String UTF8 = "UTF-8";
    private static final String CONTENT_DISPOSITION = "Content-Disposition";
    private static final String CONTENT_TYPE = "Content-Type";
    private String formDataBoundary;
    private Map<String, List<BitmapProperty>> imageAttachments;
    private Map<String, List<File>> fileAttachments;
    private Map<String, List<Uri>> uriAttachments;
    private RequestFinishedListener taskFinishedListener;
    private int TYPE_JSON = 0;
    private int TYPE_FORM = 1;
    private int TYPE_MULTIPART = 2;
    private Context context;
    private InsertBackgroundProgress backgroundProgress;
    private View progressView;
    private AlertDialog dialog;
    private ProgressViewListener progressViewListener;
    private OnUpdateProgressListener onUpdateProgress;

    public AsyncRequest( String url, RequestFinishedListener finishedListener) {
        try {
            requestUrl = url.trim();
        } catch (Exception e) {
            e.printStackTrace();
        }
        requestMethod = POST_METHOD;
        requestProperty = new ArrayList<>();
        bodyForm = new ArrayList<>();
        imageAttachments = new HashMap<>();
        fileAttachments = new HashMap<>();
        uriAttachments = new HashMap<>();
        taskFinishedListener = finishedListener;
    }


    /**
     * HTTP request method, available as properties
     * @param method POST, GET, etc
     * @return AsyncRequest
     */
    public AsyncRequest setMethod( String method) {
        requestMethod = method;
        return this;
    }

    /**
     * get HTTP request method
     * @return String method
     */
    private String getMethod() {
        if (requestMethod == null || requestMethod.trim().isEmpty()) {
            return POST_METHOD;
        }
        return requestMethod;
    }

    /**
     * set single header property
     * @param type header key
     * @param value header value
     * @return AsyncRequest
     */
    public AsyncRequest setHeader( String type,  String value) {
        KeyValuePair headerPair = new KeyValuePair(type, value);
        requestProperty.add(headerPair);
        return this;
    }

    /**
     * set multiple headers property
     * @param headers HashMap(String key, String value)
     * @return AsyncRequest
     */
    public AsyncRequest setHeaders(Map<String, String> headers) {
        Set<String> keys = headers.keySet();
        for (String key : keys) {
            if (key != null && headers.get(key) != null) {
                KeyValuePair pair = new KeyValuePair(key, headers.get(key));
                requestProperty.add(pair);
            }
        }
        return this;
    }

    /**
     * JSON header on Accept and Content-Type with Authorization param
     * @param authorization Authorization header value
     * @return AsyncRequest
     */
    public AsyncRequest useJsonHeadersWithAuthorization(String authorization) {
        requestProperty = getJsonHeaders();
        if (authorization != null && !authorization.trim().isEmpty()) {
            requestProperty.add(new KeyValuePair(HEADER_AUTH, authorization));
        }
        return this;
    }

    private List<KeyValuePair> getHeaders() {
        return requestProperty;
    }

    private List<KeyValuePair> getJsonHeaders() {
        List<KeyValuePair> defaultHeader = new ArrayList<>();
        defaultHeader.add(new KeyValuePair(HEADER_ACCEPT, CONTENT_TYPE_JSON));
        defaultHeader.add(new KeyValuePair(HEADER_CONTENT_TYPE, CONTENT_TYPE_JSON));
        return defaultHeader;
    }

    public AsyncRequest setTimeout(int timeout) {
        requestTimeout = timeout;
        return this;
    }

    private int getTimeout() {
        return requestTimeout;
    }

    /**
     * body for JSON content type in JSON string
     * @param body JSON string
     * @return AsyncRequest
     */
    public AsyncRequest setBodyRaw( String body) {
        this.rawBodyString = body;
        return this;
    }

    /**
     * body for JSON content type in byte array
     * @param body JSON in byte array
     * @return AsyncRequest
     */
    public AsyncRequest setBodyRaw( byte[] body) {
        this.rawBodyBytes = body;
        return this;
    }

    /**
     * Body for x-www-form-urlencoded or multipart form-data content type
     * @param field form field name
     * @param value form value
     * @return AsyncRequest
     */
    public AsyncRequest setBodyForm( String field,  Object value) {
        KeyValuePair body = new KeyValuePair(field, value);
        this.bodyForm.add(body);
        return this;
    }

    /**
     * add loading view on active activity
     * @param container activity where progress display
     * @param view custom view
     * @param listener finishing listener
     * @return AsyncRequest
     */
    public AsyncRequest addLoadingDialog( Activity container,  View view, ProgressViewListener listener) {
        this.loadingContainer = container;
        this.progressView = view;
        this.progressViewListener = listener;
        return this;
    }

    /**
     * add more progress while task still on background and if request is success
     * @param backgroundProgress InsertBackgroundProgress insert addition if task success progress on background
     * @return AsyncRequest
     */
    public AsyncRequest setMoreProgress( InsertBackgroundProgress backgroundProgress) {
        this.backgroundProgress = backgroundProgress;
        return this;
    }

    /**
     * Attachment for Multipart/form-data content type in Bitmap object
     * @param field form field name
     * @param bitmapProperties array bitmap attachment
     * @return AsyncRequest
     */
    public AsyncRequest addAttachments(String field, BitmapProperty... bitmapProperties) {
        List<BitmapProperty> images = Arrays.asList(bitmapProperties);
        imageAttachments.put(field, images);
        return this;
    }

    /**
     * Attachment for Multipart/form-data content type in File
     * @param context Context
     * @param field form field name
     * @param files file attachment
     * @return AsyncRequest
     */
    public AsyncRequest addAttachments(Context context, String field, File... files) {
        this.context = context;
        List<File> data = Arrays.asList(files);
        fileAttachments.put(field, data);
        return this;
    }

    /**
     * Attachment for Multipart/form-data content type in File
     * @param context Context
     * @param field form field name
     * @param uris Uri file attachment
     * @return AsyncRequest
     */
    public AsyncRequest addAttachments(Context context, String field, Uri... uris) {
        this.context = context;
        List<Uri> data = Arrays.asList(uris);
        uriAttachments.put(field, data);
        return this;
    }

    /**
     * create random string
     * @param length length random
     * @return String
     */
    public static String generateRandomString(int length) {
        Random rand = new Random();
        String characters = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
        String unique = "";
        for (Integer i = 0; i <= length; i++) {
            unique += characters.charAt(rand.nextInt(characters.length()));
        }
        return unique;
    }

    public AsyncRequest setOnUpdateProgress(OnUpdateProgressListener onUpdateProgress) {
        this.onUpdateProgress = onUpdateProgress;
        return this;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        if (requestUrl != null) {
            if (loadingContainer != null) {
                if (progressView != null) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(loadingContainer);
                    dialog = builder.setView(progressView).create();
                    if (progressViewListener != null) {
                        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                            @Override
                            public void onDismiss(DialogInterface dialogInterface) {
                                progressViewListener.onProgressFinished(progressView);
                            }
                        });
                    }
                    dialog.show();
                }
            }
            String contentType = CONTENT_TYPE_JSON;
            if (this.bodyForm.size() > 0) {
                contentType = CONTENT_TYPE_FORM;
            }
            if (imageAttachments.size() > 0 || fileAttachments.size() > 0 || uriAttachments.size() > 0) {
                formDataBoundary = generateRandomString(50);
                contentType = CONTENT_TYPE_MULTIPART + "; " + BOUNDARY + "=" + formDataBoundary;
            }
            requestProperty.add(new KeyValuePair(HEADER_CONTENT_TYPE, contentType));
            try {
                URL link = new URL(requestUrl);
                connection = (HttpURLConnection) link.openConnection();
                connection.setDoInput(true);
                connection.setRequestMethod(getMethod());
                for (KeyValuePair header : getHeaders()) {
                    String property = (String) header.getValue();
                    connection.setRequestProperty(header.getKey(), property);
                }
                if (getTimeout() > 0) {
                    connection.setConnectTimeout(getTimeout());
                }

                switch (connection.getRequestMethod().toUpperCase(Locale.US)) {
                    case PUT_METHOD:
                    case POST_METHOD:
                    case PATCH_METHOD:
                    case DELETE_METHOD:
                        connection.setDoOutput(true);
                        break;
                }
            } catch (IOException e) {
                dismissProgressDialog();
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onProgressUpdate(Long... values) {
        super.onProgressUpdate(values);
        if (onUpdateProgress != null) {
            onUpdateProgress.onUpdateProgress(values);
        }
    }

    @Override
    protected void onCancelled(byte[] bytes) {
        super.onCancelled(bytes);
    }

    @Override
    protected void onCancelled() {
        super.onCancelled();
        try {
            if (connection != null) {
                connection.disconnect();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected byte[] doInBackground(Void... voids) {
        byte[] data = new byte[0];
        if (connection != null) {
            try {
                int reqContent = getContentType();
                connection.connect();
                if (connection.getDoOutput()) {
                    OutputStream out = new BufferedOutputStream(connection.getOutputStream());
                    if (reqContent == TYPE_JSON) {
                        writeJsonBody(out);
                    } else if (reqContent == TYPE_FORM) {
                        writeFormBody(out);
                    } else if (reqContent == TYPE_MULTIPART){
                        writeFormDataBody(out);
                    }
                    out.flush();
                    out.close();
                }

                boolean success = false;
                switch (connection.getResponseCode()) {
                    case HttpURLConnection.HTTP_OK:
                    case HttpURLConnection.HTTP_ACCEPTED:
                    case HttpURLConnection.HTTP_CREATED:
                    case HttpURLConnection.HTTP_NO_CONTENT:
                        success = true;
                        break;
                }

                InputStream in = success ? connection.getInputStream() : connection.getErrorStream();
                data = convertInputStreamToByteArray(in);
                if (success && backgroundProgress != null) {
                    backgroundProgress.onBackgroundSuccess(data);
                }
            } catch (IOException e) {
                e.printStackTrace();
                connection.disconnect();
                dismissProgressDialog();
                return  new byte[0];
            } finally {
                connection.disconnect();
            }
        }
        dismissProgressDialog();
        return data;
    }

    @Override
    protected void onPostExecute(byte[] bytes) {
        super.onPostExecute(bytes);
        dismissProgressDialog();
        if (taskFinishedListener != null) {
            taskFinishedListener.onFinished(bytes);
        }
    }

    private void dismissProgressDialog() {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
    }

    private int getContentType() {
        int result = TYPE_JSON;
        try {
            if (connection != null) {
                Map<String, List<String>> props = connection.getRequestProperties();
                for (String key : props.keySet()) {
                    if (key.toLowerCase(Locale.US).equals(HEADER_CONTENT_TYPE.toLowerCase(Locale.US))) {
                        List<String> listHead = props.get(key);
                        for (String value : listHead) {
                            if (value.toLowerCase(Locale.US).equals(CONTENT_TYPE_JSON.toLowerCase(Locale.US))) {
                                result = TYPE_JSON;
                            } else if (value.toLowerCase(Locale.US).equals(CONTENT_TYPE_FORM.toLowerCase(Locale.US))) {
                                result = TYPE_FORM;
                            } else {
                                result = TYPE_MULTIPART;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    private void writeJsonBody(OutputStream os) {
        try {
            if (rawBodyString != null) {
                os.write(rawBodyString.getBytes());
            }
            if (rawBodyBytes != null) {
                os.write(rawBodyBytes);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void writeFormBody(OutputStream os) {
        try {
            List<String> body = new ArrayList<>();
            for (KeyValuePair data : bodyForm) {
                String field = data.getKey() + "=" + data.getValue();
                body.add(field);
            }
            if (body.size() > 0) {
                String form = TextUtils.join("&", body);
                String encoded = URLEncoder.encode(form, UTF8);
                os.write(encoded.getBytes());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void writeFormDataBody(OutputStream os) {
        if (formDataBoundary != null) {
            try {
                for (KeyValuePair pair : bodyForm) {
                    os.write(openBoundary());
                    os.write(contentDisposition(pair.getKey(), null));
                    os.write(BREAK.getBytes(UTF8));
                    String value = pair.getValue() + BREAK;
                    os.write(value.getBytes(UTF8));
                }

                for (String keyBitmap : imageAttachments.keySet()) {
                    List<BitmapProperty> listBitmap = imageAttachments.get(keyBitmap);
                    for (BitmapProperty bitmap : listBitmap) {
                        os.write(openBoundary());
                        os.write(contentDisposition(keyBitmap, bitmap.getFilename()));
                        os.write(contentTypeBoundary(bitmap.getMimeType()));

                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        bitmap.getBitmap().compress(bitmap.getCompressFormat(), 100, baos);
                        os.write(baos.toByteArray());
                        baos.close();
                        os.write(BREAK.getBytes(UTF8));
                    }
                }

                if (context != null) {
                    for (String fieldName : fileAttachments.keySet()) {
                        List<File> files = fileAttachments.get(fieldName);
                        for (File file : files) {
                            String contentType = getFileMimeType(context, Uri.fromFile(file));
                            if (contentType != null) {
                                os.write(openBoundary());
                                os.write(contentDisposition(fieldName, file.getName()));
                                os.write(contentTypeBoundary(contentType));
                                os.write(getFileByteArray(file));
                                os.write(BREAK.getBytes(UTF8));
                            }
                        }
                    }

                    for (String keyUri : uriAttachments.keySet()) {
                        List<Uri> uris = uriAttachments.get(keyUri);
                        for (Uri uri : uris) {
                            String contentTypeUri = getFileMimeType(context, uri);
                            String fileNameUri = getFilenameFromUri(context, uri);
                            if (contentTypeUri != null) {
                                os.write(openBoundary());
                                os.write(contentDisposition(keyUri, fileNameUri));
                                os.write(contentTypeBoundary(contentTypeUri));
                                os.write(getUriByteArray(context, uri));
                                os.write(BREAK.getBytes(UTF8));
                            }
                        }
                    }
                }

                os.write(closeBoundary());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private byte[] openBoundary() {
        if (formDataBoundary != null) {
            try {
                return (DOUBLE_DASH + formDataBoundary + BREAK).getBytes(UTF8);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return new byte[0];
    }

    private byte[] closeBoundary() {
        if (formDataBoundary != null) {
            try {
                return (DOUBLE_DASH + formDataBoundary + DOUBLE_DASH + BREAK).getBytes(UTF8);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return new byte[0];
    }

    private byte[] contentDisposition( String fieldName, String filename) {
        if (formDataBoundary != null) {
            try {
                String content = CONTENT_DISPOSITION + ": form-data; name=\"" + fieldName + "\"";
                if (filename != null) {
                    content += "; filename=\"" + filename + "\"";
                }
                content += BREAK;
                return content.getBytes(UTF8);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return new byte[0];
    }

    private byte[] contentTypeBoundary(String contentType) {
        try {
            String type = CONTENT_TYPE + ": " + contentType + BREAK + BREAK;
            return type.getBytes();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new byte[0];
    }

    private byte[] getFileByteArray(File file) {
        if (file.exists()) {
            try {
                int size = (int) file.length();
                byte[] container = new byte[size];
                FileInputStream fis = new FileInputStream(file);
                BufferedInputStream bis = new BufferedInputStream(fis);
                bis.read(container, 0, container.length);
                bis.close();
                return container;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return new byte[0];
    }

    private byte[] getUriByteArray(Context context, Uri uri) {
        try {
            InputStream is = context.getContentResolver().openInputStream(uri);
            return convertInputStreamToByteArray(is);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new byte[0];
    }

    private byte[] convertInputStreamToByteArray(InputStream stream) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
            int len;
            if (stream != null) {
                while ((len = stream.read(buffer)) != -1) {
                    baos.write(buffer, 0, len);
                }
                byte[] result = baos.toByteArray();
                baos.close();
                return result;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new byte[0];
    }

    public static String getFileMimeType(Context context, Uri uri) {
        String mimeType = null;
        try {
            if (uri.getScheme().equals(ContentResolver.SCHEME_CONTENT)) {
                ContentResolver resolver = context.getContentResolver();
                mimeType = resolver.getType(uri);
            } else {
                String ext = MimeTypeMap.getFileExtensionFromUrl(uri.toString());
                mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext.toLowerCase(Locale.US));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return mimeType;
    }

    public static String getFilenameFromUri(Context context, Uri uri) {
        String result = null;
        try {
            String[] projection = { MediaStore.Images.ImageColumns.DISPLAY_NAME };
            Cursor cursor = context.getContentResolver().query(uri, projection, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int colIndex = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DISPLAY_NAME);
                result = cursor.getString(colIndex);
                cursor.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    private static class KeyValuePair {
        private String key;
        private Object value;

        KeyValuePair(String key, Object value){
            setKey(key);
            setValue(value);
        }

        void setKey(String key) {
            this.key = key;
        }

        void setValue(Object value) {
            this.value = value;
        }

        String getKey() {
            return key;
        }

        Object getValue() {
            return value;
        }
    }

    public interface OnUpdateProgressListener {
        void onUpdateProgress(Long... progress);
    }

    public interface RequestFinishedListener {
        void onFinished(byte[] result);
    }

    public interface InsertBackgroundProgress {
        /* Don't do any interaction with UI Thread */
        void onBackgroundSuccess(byte[] data);
    }

    public interface ProgressViewListener {
        void onProgressFinished(View view);
    }
}
