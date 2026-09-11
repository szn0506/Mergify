package com.szn.merger.Utils.Signing;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.textfield.TextInputEditText;
import com.szn.merger.CustomSwitchItem;
import com.szn.merger.R;
import com.szn.merger.ThemeManager;
import com.szn.merger.Utils.CustomView.CustomDropdownItem;
import com.szn.merger.Utils.RadioAdapter;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.StringJoiner;

public class SigningActivity extends AppCompatActivity {
    private CustomSwitchItem signSwitch;
    TextInputEditText keystoreNameInput, alias, password, confirmPassword, importPassword, commonNameInput, organizationInput, organizationalUnitInput, localityInput, stateInput, countryInput;
    private MaterialCardView signSchemes;
    private static MaterialCheckBox V1, V2, V3, V4, V3_1;
    private TextView currentSchemes;
    private MaterialToolbar toolbar;
    private RecyclerView keystoreRecycler;
    private KeystoreAdapter adapter;
    private MaterialButton btnGenerate;
    private MaterialButton btnImport;
    private String keystoreName, aliasName, pass, confirm, importPass;
    private Uri selectedKeystoreUri;
    private String selectedKeystoreType, selectedKeystoreName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applyTheme(this);
        ThemeManager.applyLanguage(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.signing_layout);
        initView();
        setupListener();
        setupRecycler();
        loadState();
        updatePlaceholder();
    }

    private void initView() {
        signSwitch = findViewById(R.id.SignSwitch);
        signSchemes = findViewById(R.id.signSchemes);
        currentSchemes = findViewById(R.id.currentSchemes);
        toolbar = findViewById(R.id.toolbar);
        keystoreRecycler = findViewById(R.id.keystoreRecycler);
        btnGenerate = findViewById(R.id.btnGenerate);
        btnImport = findViewById(R.id.btnImport);
    }
    private void setupListener() {
        signSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SigningManager.setSignEnabled(this, isChecked);
        });
        signSchemes.setOnClickListener(v -> {
            showSchemesBottomSheet();
        });
        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        btnGenerate.setOnClickListener(v -> showGenerateBottomSheet());
        btnImport.setOnClickListener( v-> showImportBottomSheet());
    }
    private void setupRecycler() {

        KeystoreManager manager =
                new KeystoreManager(this);

        adapter =
                new KeystoreAdapter(
                        this,
                        manager.getAll()
                );

        keystoreRecycler.setLayoutManager(
                new LinearLayoutManager(this)
        );

        keystoreRecycler.setAdapter(adapter);
    }

    private boolean validateGenerateInput() {

        if (keystoreName.isEmpty()) {
            keystoreNameInput.setError("Required");
            return false;
        }
        if (aliasName.isEmpty()) {
            alias.setError("Required");
            return false;
        }

        if (pass.length() < 6) {
            password.setError("At least 6 characters");
            return false;
        }

        if (!pass.equals(confirm)) {
            confirmPassword.setError("Password doesn't match");
            return false;
        }

        return true;
    }
    private void generateKeystore(String name, String aliasName, String password, String commonName, String organization, String organizationalUnit, String locality, String state, String country, Date validityNotBefore, Date validityNotAfter) {
        try {
            KeystoreManager manager = new KeystoreManager(this);

            KeystoreGenerator generator = new KeystoreGenerator(name, aliasName, password, commonName, organization, organizationalUnit, locality, state, country, validityNotBefore, validityNotAfter);

            KeystoreManager.Item item = generator.generate(manager.getFolder());
            Log.d(
                    "KEYSTORE",
                    "Saved: " + item.fileName);
            manager.save(item);

            adapter.reload(manager.getAll());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showGenerateBottomSheet() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View bottomSheetView = LayoutInflater.from(this).inflate(R.layout.keystore_generator_bottom_sheet, null);
        bottomSheetDialog.setContentView(bottomSheetView);
        bottomSheetDialog.show();

        keystoreNameInput = bottomSheetView.findViewById(R.id.keystoreNameInput);
        alias = bottomSheetView.findViewById(R.id.aliasInput);
        password = bottomSheetView.findViewById(R.id.passwordInput);
        confirmPassword = bottomSheetView.findViewById(R.id.repeatPasswordInput);
        commonNameInput = bottomSheetView.findViewById(R.id.commonNameInput);
        organizationInput = bottomSheetView.findViewById(R.id.organizationInput);
        organizationalUnitInput = bottomSheetView.findViewById(R.id.organizationalUnitInput);
        localityInput = bottomSheetView.findViewById(R.id.localityInput);
        stateInput = bottomSheetView.findViewById(R.id.stateInput);
        countryInput = bottomSheetView.findViewById(R.id.countryInput);
        String commonName = commonNameInput.getText().toString().trim(),
                organization = organizationInput.getText().toString().trim(),
                organizationalUnit = organizationalUnitInput.getText().toString().trim(),
                locality = localityInput.getText().toString().trim(),
                state = stateInput.getText().toString().trim(),
                country = countryInput.getText().toString().trim();

        CustomDropdownItem validityInput = bottomSheetView.findViewById(R.id.validityInput);

        View popupView = LayoutInflater.from(this)
                .inflate(R.layout.validity_dropdown, null);

        RecyclerView recyclerView = popupView.findViewById(R.id.recyclerView);

        Date[] validity = {null, null};

        RadioAdapter validityAdapter = new RadioAdapter(
                Arrays.asList(getResources().getStringArray(R.array.validity_options)),
                (position, value) -> {

                    String[] parts = value.split(" ");
                    int amount = Integer.parseInt(parts[0]);
                    String unit = parts[1].toLowerCase();

                    Calendar calendar = Calendar.getInstance();

                    validity[0] = calendar.getTime();

                    if (unit.startsWith("month")) {
                        calendar.add(Calendar.MONTH, amount);
                    } else if (unit.startsWith("year")) {
                        calendar.add(Calendar.YEAR, amount);
                    }

                    validity[1] = calendar.getTime();
                }
        );

        recyclerView.setAdapter(validityAdapter);
        validityInput.showPopupWindow(popupView);

        ConstraintLayout advancedSettingsLayout = bottomSheetView.findViewById(R.id.advancedSettingsContent);
        ImageButton advancedSettingsButton = bottomSheetView.findViewById(R.id.advancedSettingsButton),
                rsaCheck = bottomSheetView.findViewById(R.id.rsaCard),
                ecCheck = bottomSheetView.findViewById(R.id.ecCard);
        advancedSettingsButton.setOnClickListener(v -> advancedSettingsLayout.setVisibility(advancedSettingsLayout.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE));
        MaterialCardView rsaCard = bottomSheetView.findViewById(R.id.rsaCard),
                ecCard = bottomSheetView.findViewById(R.id.ecCard);

        rsaCard.setOnClickListener(v -> {
            rsaCheck.setVisibility(View.VISIBLE);
            //rsaCard.setCardBackgroundColor(com.google.android.material.R.attr.colorOnSecondaryContainer);
            ecCheck.setVisibility(View.GONE);
            //ecCard.setBackgroundColor(com.google.android.material.R.attr.colorSurface);
        });
        ecCard.setOnClickListener(v -> {
            ecCheck.setVisibility(View.VISIBLE);
            //ecCard.setBackgroundColor(com.google.android.material.R.attr.colorSecondaryVariant);
            rsaCheck.setVisibility(View.GONE);
            //ecCard.setBackgroundColor(com.google.android.material.R.attr.colorSurface);
        });

        MaterialButton generateBtn = bottomSheetView.findViewById(R.id.btnGenerate);

        generateBtn.setOnClickListener(v -> {
            keystoreName = keystoreNameInput.getText().toString().trim();
            aliasName = alias.getText().toString().trim();
            pass = password.getText().toString().trim();
            confirm = confirmPassword.getText().toString().trim();

            if (!validateGenerateInput()) return;

            generateKeystore(keystoreName, aliasName, pass, commonName, organization, organizationalUnit, locality, state, country, validity[0], validity[1]);

            bottomSheetDialog.dismiss();
        });
    }

    private final ActivityResultLauncher<Intent> keystorePicker =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {

                        if (result.getResultCode() == RESULT_OK
                                && result.getData() != null) {

                            selectedKeystoreUri = result.getData().getData();

                            selectedKeystoreName = getFileName(selectedKeystoreUri);

                            String path = selectedKeystoreUri.getPath();

                            if (path != null && path.endsWith(".jks")) {
                                selectedKeystoreType = "JKS";
                            } else {
                                selectedKeystoreType = "PKCS12";
                            }

                            Toast.makeText(
                                    this,
                                    "Keystore selected",
                                    Toast.LENGTH_SHORT
                            ).show();
                        }
                    }
            );


    private void openSAF() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        keystorePicker.launch(intent);
    }

    private boolean validateImport() {
        if (selectedKeystoreUri == null) {
            Toast.makeText(
                    this,
                    "Select keystore first",
                    Toast.LENGTH_SHORT
            ).show();
            return false;
        }


        String password =
                importPassword.getText()
                        .toString();


        if (password.isEmpty()) {
            importPassword.setError(
                    "Password required"
            );
            return false;
        }

        return true;
    }

    private void importKeystore() {
        try {
            KeystoreManager manager = new KeystoreManager(this);
            KeystoreImporter importer = new KeystoreImporter(this);
            KeystoreManager.Item item = importer.importKeystore(selectedKeystoreUri, selectedKeystoreName, importPass, selectedKeystoreType);
            manager.save(item);
            adapter.reload(manager.getAll());

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String getFileName(Uri uri) {
        String result = null;

        if ("content".equals(uri.getScheme())) {
            try (android.database.Cursor cursor =
                         getContentResolver().query(
                                 uri,
                                 null,
                                 null,
                                 null,
                                 null
                         )) {

                if (cursor != null && cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(
                            android.provider.OpenableColumns.DISPLAY_NAME
                    );

                    if (index >= 0) {
                        result = cursor.getString(index);
                    }
                }
            }
        }

        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');

            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }

        return result;
    }

    private void showImportBottomSheet() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View bottomSheetView = LayoutInflater.from(this).inflate(R.layout.keystore_import_bottom_sheet, null);
        bottomSheetDialog.setContentView(bottomSheetView);
        bottomSheetDialog.show();

        importPassword = bottomSheetDialog.findViewById(R.id.password);
        MaterialCardView importCard = bottomSheetDialog.findViewById(R.id.importKeystore);
        MaterialButton importBtn = bottomSheetDialog.findViewById(R.id.btnImport);
        importCard.setOnClickListener(v -> openSAF());

        importBtn.setOnClickListener(v -> {
            importPass = importPassword.getText().toString().trim();
            if (!validateImport()) return;
            importKeystore();
            bottomSheetDialog.dismiss();
        });
    }

    private void showSchemesBottomSheet() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View bottomSheetView = LayoutInflater.from(this).inflate(R.layout.sign_schemes_sheet, null);
        bottomSheetDialog.setContentView(bottomSheetView);
        bottomSheetDialog.show();

        V1 = bottomSheetView.findViewById(R.id.checkV1);
        V2 = bottomSheetView.findViewById(R.id.checkV2);
        V3 = bottomSheetView.findViewById(R.id.checkV3);
        V4 = bottomSheetView.findViewById(R.id.checkV4);
        V3_1 = bottomSheetView.findViewById(R.id.checkV3_1);
        MaterialButton doneButton = bottomSheetView.findViewById(R.id.doneButton);
        restoreState();
        MaterialCardView V1Card = bottomSheetView.findViewById(R.id.V1Card),
                V2Card = bottomSheetView.findViewById(R.id.V2Card),
                V3Card = bottomSheetView.findViewById(R.id.V3Card),
                V4Card = bottomSheetView.findViewById(R.id.V4Card),
                V3_1Card = bottomSheetView.findViewById(R.id.V3_1Card);

        V1Card.setOnClickListener(v -> V1.performClick());
        V2Card.setOnClickListener(v -> V2.performClick());
        V3Card.setOnClickListener(v -> V3.performClick());
        V4Card.setOnClickListener(v -> V4.performClick());
        V3_1Card.setOnClickListener(v -> V3_1.performClick());

        V1.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SigningManager.setV1Enabled(this, isChecked);
            updatePlaceholder();
        });

        V2.setOnCheckedChangeListener((buttonView, isChecked) ->
                SigningManager.setV2Enabled(this, isChecked));

        V3.setOnCheckedChangeListener((buttonView, isChecked) ->
                SigningManager.setV3Enabled(this, isChecked));

        V4.setOnCheckedChangeListener((buttonView, isChecked) ->
                SigningManager.setV4Enabled(this, isChecked));
        V3_1.setOnCheckedChangeListener((buttonView, isChecked) ->
                SigningManager.setV3_1Enabled(this, isChecked));
        doneButton.setOnClickListener(v -> {
            updatePlaceholder();
            bottomSheetDialog.dismiss();
        });
    }
    private void restoreState() {
        V1.setChecked(SigningManager.isV1Enabled(this));
        V2.setChecked(SigningManager.isV2Enabled(this));
        V3.setChecked(SigningManager.isV3Enabled(this));
        V4.setChecked(SigningManager.isV4Enabled(this));
        V3_1.setChecked(SigningManager.isV3_1Enabled(this));
    }

    private void updatePlaceholder() {
        StringJoiner schemes = new StringJoiner(", ");

        if (SigningManager.isV1Enabled(this)) schemes.add("V1");
        if (SigningManager.isV2Enabled(this)) schemes.add("V2");
        if (SigningManager.isV3Enabled(this)) schemes.add("V3");
        if (SigningManager.isV3_1Enabled(this)) schemes.add("V3.1");
        if (SigningManager.isV4Enabled(this)) schemes.add("V4");

        currentSchemes.setText(schemes.length() > 0 ? schemes.toString() : "None");
    }
    private void loadState() {
        signSwitch.setChecked(SigningManager.isSignEnabled(this));
    }
}
