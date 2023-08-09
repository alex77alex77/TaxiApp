package com.alexei.taxiapp;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class MultiSpinner extends androidx.appcompat.widget.AppCompatSpinner {
    private CharSequence[] entries;
    private boolean[] selected;
    private String textSelectedItems = "";


    private SelectedListener onSelectedListener;

    public interface SelectedListener {
        void onItemsSelected(CharSequence[] selEntries);

    }

    public void setSelectedListener(SelectedListener listener) {
        this.onSelectedListener = listener;
    }


    public String getTextSelectedItems() {
        return textSelectedItems;
    }

    public void setTextSelectedItems(String textSelectedItems) {
        this.textSelectedItems = textSelectedItems;
        loadSpinner(textSelectedItems);

    }


    public void setError(View v, CharSequence s) {
        TextView name = (TextView) v.findViewById(android.R.id.text1);
        name.setError(s);
    }

    private void loadSpinner(String textSelectedItems) {
        String[] words = textSelectedItems.split(",");
        for (int iw = 0; iw < words.length; iw++) {
            for (int ie = 0; ie < entries.length; ie++) {
                if (entries[ie].equals(words[iw].trim())) {
                    selected[ie] = true;

                }
            }
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getContext(),
                R.layout.spiner_item, new String[]{textSelectedItems.toString()});
        setAdapter(adapter);
    }


    public MultiSpinner(Context context, AttributeSet attrs) {
        super(context, attrs);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.MultiSpinner);
        entries = a.getTextArray(R.styleable.MultiSpinner_android_entries);
        if (entries != null) {
            selected = new boolean[entries.length]; // false-filled by default
            loadSpinner(textSelectedItems);
        }
        a.recycle();

    }

    private DialogInterface.OnMultiChoiceClickListener mOnMultiChoiceClickListener = new DialogInterface.OnMultiChoiceClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int index, boolean isChecked) {
            selected[index] = isChecked;
        }
    };

    private DialogInterface.OnClickListener mOnClickListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            StringBuilder spinnerBuilder = new StringBuilder();

            for (int i = 0; i < entries.length; i++) {
                if (selected[i]) {
                    spinnerBuilder.append(entries[i]);
                    spinnerBuilder.append(", ");

                }
            }


            if (spinnerBuilder.length() > 2) {
                spinnerBuilder.setLength(spinnerBuilder.length() - 2);
            }

            ArrayAdapter<String> adapter = new ArrayAdapter<String>(getContext(),
                    R.layout.spiner_item, new String[]{spinnerBuilder.toString()});
            setAdapter(adapter);
            textSelectedItems = spinnerBuilder.toString();
//            if (listener != null) {
//                listener.onItemsSelected(selected);
//            }
//            onSelectedListener.onItemsSelected(new String[]{spinnerBuilder.toString()});
            dialog.dismiss();
        }
    };

    @Override
    public boolean performClick() {
        new AlertDialog.Builder(getContext())
                .setMultiChoiceItems(entries, selected, mOnMultiChoiceClickListener)
                .setPositiveButton(android.R.string.ok, mOnClickListener)
                .show();
        return true;
    }


}


