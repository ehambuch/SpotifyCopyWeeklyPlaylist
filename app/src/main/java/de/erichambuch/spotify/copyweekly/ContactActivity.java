package de.erichambuch.spotify.copyweekly;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.widget.TextView;

/**
 * Activity kann per HTML-Link aufgerufen werden, um Kontaktdaten anzuzugen.
 */
public class ContactActivity extends Activity {

    @Override
    public void onStart() {
        super.onStart();
        AlertDialog.Builder builder = new AlertDialog.Builder(ContactActivity.this);
        builder.setTitle(R.string.app_name);
        builder.setMessage(Html.fromHtml(BuildConfig.IMPRESSUM, Html.FROM_HTML_MODE_COMPACT));
        builder.setNeutralButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
                ContactActivity.this.finish();
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
        TextView view = (TextView)dialog.findViewById(android.R.id.message);
        if (view != null ) view.setMovementMethod(LinkMovementMethod.getInstance()); // make links clickable
    }
}
