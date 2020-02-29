package com.ajit.worexpo;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;

import com.ajit.worexpo.adpter.NotesAdapter;
import com.ajit.worexpo.db.DatabaseHelper;
import com.ajit.worexpo.model.Note;
import com.ajit.worexpo.utils.MyDividerItemDecoration;
import com.ajit.worexpo.utils.RecyclerTouchListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class MedicineActivity extends AppCompatActivity {


    private NotesAdapter mAdapter;
    private List<Note> notesList = new ArrayList<>();
    private CoordinatorLayout coordinatorLayout;
    private RecyclerView recyclerView;
    private TextView noNotesView;

    private DatabaseHelper db;

    private static final String TAG = "CapturePicture";
    static final int REQUEST_PICTURE_CAPTURE = 1;
    private ImageView image;
    private String pictureFilePath;
    private String deviceIdentifier;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_medicine);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        recyclerView = findViewById(R.id.recycler_view);
        noNotesView = findViewById(R.id.empty_notes_view);
        image = findViewById(R.id.picture);

        db = new DatabaseHelper(this);

        //notesList.addAll(db.getAllNotes());

        if (db.getNotesCount() > 0) {
            db.deleteAllNotes();
        } else {
            noNotesView.setVisibility(View.VISIBLE);
        }

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //showAddDialog();
                showAddDialogImg();
            }
        });

        mAdapter = new NotesAdapter(this, notesList);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getApplicationContext());
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.addItemDecoration(new MyDividerItemDecoration(this, LinearLayoutManager.VERTICAL, 16));
        recyclerView.setAdapter(mAdapter);

        toggleEmptyNotes();

        recyclerView.addOnItemTouchListener(new RecyclerTouchListener(this,
                recyclerView, new RecyclerTouchListener.ClickListener() {
            @Override
            public void onClick(View view, final int position) {
            }

            @Override
            public void onLongClick(View view, int position) {
                showActionsDialog(position);
            }
        }));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_save_medicines, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Toast.makeText(this, "Selected Item: " +item.getTitle(), Toast.LENGTH_SHORT).show();
        switch (item.getItemId()) {
            case R.id.save_medicine:
                if (db.getNotesCount() > 0) {
                    Toast.makeText(this, "Prescription added\nyour order id is\n"+generateTransactionID(), Toast.LENGTH_SHORT).show();
                    db.deleteAllNotes();
                    finish();
                } else if(pictureFilePath!=null)
                {
                    Toast.makeText(this, "Prescription added\nyour order id is\n"+generateTransactionID(), Toast.LENGTH_SHORT).show();
                    if (db.getNotesCount() > 0)
                        db.deleteAllNotes();
                    finish();
                }else{
                    Toast.makeText(this, "Please add prescription", Toast.LENGTH_SHORT).show();
                }
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Inserting new note in db
     * and refreshing the list
     */
    private void createNote(String note, String qty) {
        // inserting note in db and getting
        // newly inserted note id
        long id = db.insertNote(note,qty);

        // get the newly inserted note from db
        Note n = db.getNote(id);

        if (n != null) {
            // adding new note to array list at 0 position
            notesList.add(0, n);

            // refreshing the list
            mAdapter.notifyDataSetChanged();

            toggleEmptyNotes();
        }
    }

    /**
     * Updating note in db and updating
     * item in the list by its position
     */
    private void updateNote(String note, String qty, int position) {
        Note n = notesList.get(position);
        // updating note text
        n.setNote(note);
        n.setQty(qty);

        // updating note in db
        db.updateNote(n);

        // refreshing the list
        notesList.set(position, n);
        mAdapter.notifyItemChanged(position);

        toggleEmptyNotes();
    }

    /**
     * Deleting note from SQLite and removing the
     * item from the list by its position
     */
    private void deleteNote(int position) {
        // deleting the note from db
        db.deleteNote(notesList.get(position));

        // removing the note from the list
        notesList.remove(position);
        mAdapter.notifyItemRemoved(position);

        toggleEmptyNotes();
    }

    /**
     * Opens dialog with Edit - Delete options
     * Edit - 0
     * Delete - 0
     */
    private void showActionsDialog(final int position) {
        CharSequence colors[] = new CharSequence[]{"Edit", "Delete"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Choose option");
        builder.setItems(colors, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which == 0) {
                    showNoteDialog(true, notesList.get(position), position);
                } else {
                    deleteNote(position);
                }
            }
        });
        builder.show();
    }


    /**
     * Shows alert dialog with EditText options to enter / edit
     * a note.
     * when shouldUpdate=true, it automatically displays old note and changes the
     * button text to UPDATE
     */
    private void showNoteDialog(final boolean shouldUpdate, final Note note, final int position) {
        LayoutInflater layoutInflaterAndroid = LayoutInflater.from(getApplicationContext());
        View view = layoutInflaterAndroid.inflate(R.layout.note_dialog, null);

        AlertDialog.Builder alertDialogBuilderUserInput = new AlertDialog.Builder(MedicineActivity.this);
        alertDialogBuilderUserInput.setView(view);

        final EditText inputNote = view.findViewById(R.id.medicine_name);
        final EditText inputQty = view.findViewById(R.id.medicine_quantity);
        TextView dialogTitle = view.findViewById(R.id.dialog_title);
        dialogTitle.setText(!shouldUpdate ? getString(R.string.lbl_new_prescription_title) : getString(R.string.lbl_edit_prescription_title));

        if (shouldUpdate && note != null) {
            inputNote.setText(note.getNote());
            inputQty.setText(note.getQty());
        }
        alertDialogBuilderUserInput
                .setCancelable(false)
                .setPositiveButton(shouldUpdate ? "update" : "save", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialogBox, int id) {

                    }
                })
                .setNegativeButton("cancel",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialogBox, int id) {
                                dialogBox.cancel();
                            }
                        });

        final AlertDialog alertDialog = alertDialogBuilderUserInput.create();
        alertDialog.show();

        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Show toast message when no text is entered
                if (TextUtils.isEmpty(inputNote.getText().toString())) {
                    Toast.makeText(MedicineActivity.this, "Enter Medicine Name!", Toast.LENGTH_SHORT).show();
                    return;
                } else if (TextUtils.isEmpty(inputQty.getText().toString())) {
                    Toast.makeText(MedicineActivity.this, "Enter Qty!", Toast.LENGTH_SHORT).show();
                    return;
                } else {
                    alertDialog.dismiss();
                }

                // check if user updating note
                if (shouldUpdate && note != null) {
                    // update note by it's id
                    updateNote(inputNote.getText().toString(), inputQty.getText().toString(), position);
                } else {
                    // create new note
                    createNote(inputNote.getText().toString(),inputQty.getText().toString());
                }
            }
        });
    }

    /**
     * Toggling list and empty notes view
     */
    private void toggleEmptyNotes() {
        // you can check notesList.size() > 0

        if (db.getNotesCount() > 0) {
            noNotesView.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            image.setVisibility(View.GONE);
        } else if(pictureFilePath!=null) {
            recyclerView.setVisibility(View.GONE);
            noNotesView.setVisibility(View.GONE);
            image.setVisibility(View.VISIBLE);
        }else {
            noNotesView.setVisibility(View.VISIBLE);
        }
    }

    private void showAddDialog() {
        CharSequence colors[] = new CharSequence[]{"Add Manually", "Scan"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Choose option");
        builder.setItems(colors, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which == 0) {
                    showNoteDialog(false, null, -1);
                } else {
                    Toast.makeText(MedicineActivity.this, "Coming Soon", Toast.LENGTH_SHORT).show();
                }
            }
        });
        builder.show();
    }

    private void showAddDialogImg() {
        LayoutInflater layoutInflaterAndroid = LayoutInflater.from(getApplicationContext());
        View view = layoutInflaterAndroid.inflate(R.layout.add_dialog, null);

        AlertDialog.Builder alertDialogBuilderUserInput = new AlertDialog.Builder(MedicineActivity.this);
        alertDialogBuilderUserInput.setView(view);

        final LinearLayout addManually = view.findViewById(R.id.addManually);
        final LinearLayout scan = view.findViewById(R.id.scan);

        final AlertDialog alertDialog = alertDialogBuilderUserInput.create();
        alertDialog.show();

          if(!getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)){
              scan.setEnabled(false);
                }

        addManually.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertDialog.dismiss();
                showNoteDialog(false, null, -1);
            }
        });
        scan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertDialog.dismiss();
                //Toast.makeText(MedicineActivity.this, "Coming Soon", Toast.LENGTH_SHORT).show();
               /* Intent intent = new Intent(MedicineActivity.this, CapturePictureActivity.class);
                startActivity(intent);*/

                getInstallationIdentifier();

                if(getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)){
                    sendTakePictureIntent();
                }
            }
        });
    }

    public String generateTransactionID(){
        Random random = new Random();
        int randomInteger = random.nextInt();
        String transID = String.valueOf(randomInteger);
        return  transID;
    }

    protected synchronized String getInstallationIdentifier() {
        if (deviceIdentifier == null) {
            SharedPreferences sharedPrefs = this.getSharedPreferences(
                    "DEVICE_ID", Context.MODE_PRIVATE);
            deviceIdentifier = sharedPrefs.getString("DEVICE_ID", null);
            if (deviceIdentifier == null) {
                deviceIdentifier = UUID.randomUUID().toString();
                SharedPreferences.Editor editor = sharedPrefs.edit();
                editor.putString("DEVICE_ID", deviceIdentifier);
                editor.commit();
            }
        }
        return deviceIdentifier;
    }

    private void sendTakePictureIntent() {

        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraIntent.putExtra( MediaStore.EXTRA_FINISH_ON_COMPLETION, true);
        if (cameraIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(cameraIntent, REQUEST_PICTURE_CAPTURE);

            File pictureFile = null;
            try {
                pictureFile = getPictureFile();
            } catch (IOException ex) {
                Toast.makeText(this,
                        "Photo file can't be created, please try again",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            if (pictureFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.ajit.android.fileprovider",
                        pictureFile);
                cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(cameraIntent, REQUEST_PICTURE_CAPTURE);
            }
        }
    }
    private File getPictureFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        String pictureFile = "WOREXPO_" + timeStamp;
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(pictureFile,  ".jpg", storageDir);
        pictureFilePath = image.getAbsolutePath();
        return image;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_PICTURE_CAPTURE && resultCode == RESULT_OK) {
            File imgFile = new File(pictureFilePath);
            if (imgFile.exists()) {
                recyclerView.setVisibility(View.GONE);
                noNotesView.setVisibility(View.GONE);
                image.setVisibility(View.VISIBLE);
                image.setImageURI(Uri.fromFile(imgFile));
            }
        }
    }

}
