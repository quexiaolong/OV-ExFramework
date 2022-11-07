package com.android.server.textclassifier;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.os.UserHandle;
import android.util.Log;
import android.util.Pair;
import com.android.server.textclassifier.IconsUriHelper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;

/* loaded from: classes2.dex */
public final class IconsContentProvider extends ContentProvider {
    private static final String MIME_TYPE = "image/png";
    private static final String TAG = "IconsContentProvider";
    private final ContentProvider.PipeDataWriter<Pair<IconsUriHelper.ResourceInfo, Integer>> mWriter = new ContentProvider.PipeDataWriter() { // from class: com.android.server.textclassifier.-$$Lambda$IconsContentProvider$2jS4meM2zcZimtuC21MGGWtYcqQ
        @Override // android.content.ContentProvider.PipeDataWriter
        public final void writeDataToPipe(ParcelFileDescriptor parcelFileDescriptor, Uri uri, String str, Bundle bundle, Object obj) {
            IconsContentProvider.this.lambda$new$0$IconsContentProvider(parcelFileDescriptor, uri, str, bundle, (Pair) obj);
        }
    };

    public /* synthetic */ void lambda$new$0$IconsContentProvider(ParcelFileDescriptor writeSide, Uri uri, String mimeType, Bundle bundle, Pair args) {
        try {
            OutputStream out = new ParcelFileDescriptor.AutoCloseOutputStream(writeSide);
            IconsUriHelper.ResourceInfo res = (IconsUriHelper.ResourceInfo) args.first;
            int userId = ((Integer) args.second).intValue();
            Drawable drawable = Icon.createWithResource(res.packageName, res.id).loadDrawableAsUser(getContext(), userId);
            getBitmap(drawable).compress(Bitmap.CompressFormat.PNG, 100, out);
            out.close();
        } catch (Exception e) {
            Log.e(TAG, "Error retrieving icon for uri: " + uri, e);
        }
    }

    @Override // android.content.ContentProvider
    public ParcelFileDescriptor openFile(Uri uri, String mode) {
        IconsUriHelper.ResourceInfo res = IconsUriHelper.getInstance().getResourceInfo(uri);
        if (res == null) {
            Log.e(TAG, "No icon found for uri: " + uri);
            return null;
        }
        try {
            Pair<IconsUriHelper.ResourceInfo, Integer> args = new Pair<>(res, Integer.valueOf(UserHandle.getCallingUserId()));
            return openPipeHelper(uri, MIME_TYPE, null, args, this.mWriter);
        } catch (IOException e) {
            Log.e(TAG, "Error opening pipe helper for icon at uri: " + uri, e);
            return null;
        }
    }

    private static Bitmap getBitmap(Drawable drawable) {
        if (drawable.getIntrinsicWidth() <= 0 || drawable.getIntrinsicHeight() <= 0) {
            throw new IllegalStateException("The icon is zero-sized");
        }
        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }

    public static boolean sameIcon(Drawable one, Drawable two) {
        ByteArrayOutputStream stream1 = new ByteArrayOutputStream();
        getBitmap(one).compress(Bitmap.CompressFormat.PNG, 100, stream1);
        ByteArrayOutputStream stream2 = new ByteArrayOutputStream();
        getBitmap(two).compress(Bitmap.CompressFormat.PNG, 100, stream2);
        return Arrays.equals(stream1.toByteArray(), stream2.toByteArray());
    }

    @Override // android.content.ContentProvider
    public String getType(Uri uri) {
        return MIME_TYPE;
    }

    @Override // android.content.ContentProvider
    public boolean onCreate() {
        return true;
    }

    @Override // android.content.ContentProvider
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        return null;
    }

    @Override // android.content.ContentProvider
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override // android.content.ContentProvider
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    @Override // android.content.ContentProvider
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }
}