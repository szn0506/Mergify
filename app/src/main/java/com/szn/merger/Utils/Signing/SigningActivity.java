package com.szn.merger.Utils.Signing;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
import com.szn.merger.CustomSwitchItem;
import com.szn.merger.R;
import com.szn.merger.ThemeManager;
import com.szn.merger.Utils.CustomView.CustomDropdownItem;
import com.szn.merger.Utils.RadioAdapter;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
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
    private String currentKeystoreType = "PKCS12";
    private String currentKeyAlgorithm = "PKCS12";

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

    private void showValidityOption(CustomDropdownItem validityInput, Date[] validity) {
        View popupView = LayoutInflater.from(this).inflate(R.layout.validity_dropdown, null);
        LinearLayout customValidity = popupView.findViewById(R.id.customValidity);
        RecyclerView recyclerView = popupView.findViewById(R.id.recyclerView);

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

        View validityDialog = getLayoutInflater().inflate(R.layout.validity_dialog, null);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(validityDialog)
                .create();

        MaterialCardView startDateCard = validityDialog.findViewById(R.id.startDateCard),
                startTimeCard = validityDialog.findViewById(R.id.startTimeCard),
                expiryDateCard = validityDialog.findViewById(R.id.expiryDateCard),
                expiryTimeCard = validityDialog.findViewById(R.id.expiryTimeCard);
        TextView startDatePreview = validityDialog.findViewById(R.id.startDateSubtitle),
                startTimePreview = validityDialog.findViewById(R.id.startTimerSubtitle),
                expiryDatePreview = validityDialog.findViewById(R.id.expiryDateSubtitle),
                expiryTimePreview = validityDialog.findViewById(R.id.expiryTimeSubtitle);

        Calendar startDate = Calendar.getInstance();
        startDate.set(Calendar.HOUR_OF_DAY, 0);
        startDate.set(Calendar.MINUTE, 0);
        startDate.set(Calendar.SECOND, 0);
        startDate.set(Calendar.MILLISECOND, 0);
        validity[0] = startDate.getTime();

        Calendar expiryDate = Calendar.getInstance();
        expiryDate.set(Calendar.HOUR_OF_DAY, 0);
        expiryDate.set(Calendar.MINUTE, 0);
        expiryDate.set(Calendar.SECOND, 0);
        expiryDate.set(Calendar.MILLISECOND, 0);
        validity[1] = expiryDate.getTime();

        startDateCard.setOnClickListener(v -> {
            MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                    .setTitleText("Select start date")
                    .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                    .build();

            datePicker.addOnPositiveButtonClickListener(selection -> {
                Calendar selected = Calendar.getInstance();
                selected.setTimeInMillis(selection);

                startDate.set(Calendar.YEAR, selected.get(Calendar.YEAR));
                startDate.set(Calendar.MONTH, selected.get(Calendar.MONTH));
                startDate.set(Calendar.DAY_OF_MONTH, selected.get(Calendar.DAY_OF_MONTH));

                validity[0] = startDate.getTime();
                startDatePreview.setText(new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(startDate.getTime()));
            });

            datePicker.show(getSupportFragmentManager(), "START_DATE");
        });

        startTimeCard.setOnClickListener(v -> {
            MaterialTimePicker timePicker = new MaterialTimePicker.Builder()
                    .setTimeFormat(TimeFormat.CLOCK_24H)
                    .setHour(0)
                    .setMinute(0)
                    .setTitleText("Select start time")
                    .build();

            timePicker.addOnPositiveButtonClickListener(v1 -> {
                startDate.set(Calendar.HOUR_OF_DAY, timePicker.getHour());
                startDate.set(Calendar.MINUTE, timePicker.getMinute());
                startDate.set(Calendar.SECOND, 0);
                startDate.set(Calendar.MILLISECOND, 0);

                validity[0] = startDate.getTime();
                startTimePreview.setText(String.format(Locale.getDefault(), "%02d:%02d:%02d", startDate.get(Calendar.HOUR_OF_DAY), startDate.get(Calendar.MINUTE), startDate.get(Calendar.SECOND)));
            });

            timePicker.show(getSupportFragmentManager(), "START_TIME");
        });

        expiryDateCard.setOnClickListener(v -> {
            MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                    .setTitleText("Select expiry date")
                    .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                    .build();

            datePicker.addOnPositiveButtonClickListener(selection -> {
                Calendar selected = Calendar.getInstance();
                selected.setTimeInMillis(selection);

                expiryDate.set(Calendar.YEAR, selected.get(Calendar.YEAR));
                expiryDate.set(Calendar.MONTH, selected.get(Calendar.MONTH));
                expiryDate.set(Calendar.DAY_OF_MONTH, selected.get(Calendar.DAY_OF_MONTH));

                validity[1] = expiryDate.getTime();
                expiryDatePreview.setText(new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(expiryDate.getTime()));
            });

            datePicker.show(getSupportFragmentManager(), "EXPIRY_DATE");
        });

        expiryTimeCard.setOnClickListener(v -> {
            MaterialTimePicker timePicker = new MaterialTimePicker.Builder()
                    .setTimeFormat(TimeFormat.CLOCK_24H)
                    .setHour(0)
                    .setMinute(0)
                    .setTitleText("Select expiry time")
                    .build();

            timePicker.addOnPositiveButtonClickListener(v1 -> {
                expiryDate.set(Calendar.HOUR_OF_DAY, timePicker.getHour());
                expiryDate.set(Calendar.MINUTE, timePicker.getMinute());
                expiryDate.set(Calendar.SECOND, 0);
                expiryDate.set(Calendar.MILLISECOND, 0);

                validity[1] = expiryDate.getTime();
                expiryTimePreview.setText(String.format(Locale.getDefault(), "%02d:%02d:%02d", expiryDate.get(Calendar.HOUR_OF_DAY), expiryDate.get(Calendar.MINUTE), expiryDate.get(Calendar.SECOND)));
            });

            timePicker.show(getSupportFragmentManager(), "EXPIRY_TIME");
        });
        customValidity.setOnClickListener(v -> dialog.show());
        Log.d("VALIDITY", "customValidity = " + customValidity);
        validityInput.showPopupWindow(popupView);
    }

    private void showKeystoreTypeDropdown(CustomDropdownItem keystoreTypeDropdown) {
        View popupView = LayoutInflater.from(this).inflate(R.layout.keystore_type_dropdown, null);

        ImageView checkJKS = popupView.findViewById(R.id.check_jks);
        ImageView checkPKCS12 = popupView.findViewById(R.id.check_pkcs12);
        ImageView checkJCEKS = popupView.findViewById(R.id.check_jceks);
        ImageView checkBKS = popupView.findViewById(R.id.check_bks);
        ImageView checkBKSV1 = popupView.findViewById(R.id.check_bks_v1);
        ImageView checkUBER = popupView.findViewById(R.id.check_uber);
        ImageView checkBCFKS = popupView.findViewById(R.id.check_bcfks);

        RelativeLayout JKS = popupView.findViewById(R.id.jks);
        RelativeLayout PKCS12 = popupView.findViewById(R.id.pkcs12);
        RelativeLayout JCEKS = popupView.findViewById(R.id.jceks);
        RelativeLayout BKS = popupView.findViewById(R.id.bks);
        RelativeLayout BKSV1 = popupView.findViewById(R.id.bks_v1);
        RelativeLayout UBER = popupView.findViewById(R.id.uber);
        RelativeLayout BCFKS = popupView.findViewById(R.id.bcfks);

        checkJKS.setVisibility(currentKeystoreType.equals("JKS") ? View.VISIBLE : View.GONE);
        checkPKCS12.setVisibility(currentKeystoreType.equals("PKCS12") ? View.VISIBLE : View.GONE);
        checkJCEKS.setVisibility(currentKeystoreType.equals("JCEKS") ? View.VISIBLE : View.GONE);
        checkBKS.setVisibility(currentKeystoreType.equals("BKS") ? View.VISIBLE : View.GONE);
        checkBKSV1.setVisibility(currentKeystoreType.equals("BKS-V1") ? View.VISIBLE : View.GONE);
        checkUBER.setVisibility(currentKeystoreType.equals("UBER") ? View.VISIBLE : View.GONE);
        checkBCFKS.setVisibility(currentKeystoreType.equals("BCFKS") ? View.VISIBLE : View.GONE);

        View.OnClickListener listener = v -> {
            currentKeystoreType = v == JKS ? "JKS" :
                    v == PKCS12 ? "PKCS12" :
                    v == JCEKS ? "JCEKS" :
                    v == BKS ? "BKS" :
                    v == BKSV1 ? "BKS-V1" :
                    v == UBER ? "UBER" :
                    "BCFKS";

            checkJKS.setVisibility(currentKeystoreType.equals("JKS") ? View.VISIBLE : View.GONE);
            checkPKCS12.setVisibility(currentKeystoreType.equals("PKCS12") ? View.VISIBLE : View.GONE);
            checkJCEKS.setVisibility(currentKeystoreType.equals("JCEKS") ? View.VISIBLE : View.GONE);
            checkBKS.setVisibility(currentKeystoreType.equals("BKS") ? View.VISIBLE : View.GONE);
            checkBKSV1.setVisibility(currentKeystoreType.equals("BKS-V1") ? View.VISIBLE : View.GONE);
            checkUBER.setVisibility(currentKeystoreType.equals("UBER") ? View.VISIBLE : View.GONE);
            checkBCFKS.setVisibility(currentKeystoreType.equals("BCFKS") ? View.VISIBLE : View.GONE);
            keystoreTypeDropdown.dismissPopupWindow();
        };
        JKS.setOnClickListener(listener);
        PKCS12.setOnClickListener(listener);
        JCEKS.setOnClickListener(listener);
        BKS.setOnClickListener(listener);
        BKSV1.setOnClickListener(listener);
        UBER.setOnClickListener(listener);
        BCFKS.setOnClickListener(listener);

        keystoreTypeDropdown.showPopupWindow(popupView);
    }
    private void handleKeyAlgorithm(View view) {
        MaterialCardView rsaCard = view.findViewById(R.id.rsaCard), ecCard = view.findViewById(R.id.ecCard);
        rsaCard.setOnClickListener(v -> {
            rsaCard.setStrokeWidth((int) (1 * getResources().getDisplayMetrics().density + 0.5f));
            rsaCard.setCardBackgroundColor(MaterialColors.getColor(rsaCard, com.google.android.material.R.attr.colorPrimaryContainer));
            currentKeyAlgorithm = "RSA";
            ecCard.setStrokeWidth(0);
            ecCard.setCardBackgroundColor(MaterialColors.getColor(ecCard, com.google.android.material.R.attr.colorSurfaceContainer));
        });

        ecCard.setOnClickListener(v -> {
            ecCard.setStrokeWidth((int) (1 * getResources().getDisplayMetrics().density + 0.5f));
            ecCard.setCardBackgroundColor(MaterialColors.getColor(ecCard, com.google.android.material.R.attr.colorPrimaryContainer));
            currentKeyAlgorithm = "EC";
            rsaCard.setStrokeWidth(0);
            rsaCard.setCardBackgroundColor(MaterialColors.getColor(rsaCard, com.google.android.material.R.attr.colorSurfaceContainer));
        });
    }
    private void showGenerateBottomSheet() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View bottomSheetView = LayoutInflater.from(this).inflate(R.layout.keystore_generator_bottom_sheet, null);

        bottomSheetDialog.setContentView(bottomSheetView);
        bottomSheetDialog.show();

        Date[] validity = {null, null};

        keystoreNameInput = bottomSheetView.findViewById(R.id.keystoreNameInput);
        alias = bottomSheetView.findViewById(R.id.aliasInput);
        password = bottomSheetView.findViewById(R.id.passwordInput);
        confirmPassword = bottomSheetView.findViewById(R.id.repeatPasswordInput);
        CustomDropdownItem keystoreTypeDropdown = bottomSheetView.findViewById(R.id.KeystoreTypeDropdown);

        showKeystoreTypeDropdown(keystoreTypeDropdown);
        handleKeyAlgorithm(bottomSheetView);

        commonNameInput = bottomSheetView.findViewById(R.id.commonNameInput);
        organizationInput = bottomSheetView.findViewById(R.id.organizationInput);
        organizationalUnitInput = bottomSheetView.findViewById(R.id.organizationalUnitInput);
        localityInput = bottomSheetView.findViewById(R.id.localityInput);
        stateInput = bottomSheetView.findViewById(R.id.stateInput);
        countryInput = bottomSheetView.findViewById(R.id.countryInput);

        MaterialButton generateBtn = bottomSheetView.findViewById(R.id.btnGenerate);

        generateBtn.setOnClickListener(v -> {
            keystoreName = keystoreNameInput.getText().toString().trim();
            aliasName = alias.getText().toString().trim();
            pass = password.getText().toString().trim();
            confirm = confirmPassword.getText().toString().trim();

            String commonName = commonNameInput.getText().toString().trim();
            String organization = organizationInput.getText().toString().trim();
            String organizationalUnit = organizationalUnitInput.getText().toString().trim();
            String locality = localityInput.getText().toString().trim();
            String state = stateInput.getText().toString().trim();
            String country = countryInput.getText().toString().trim();

            if (!validateGenerateInput()) return;

            generateKeystore(
                    keystoreName,
                    aliasName,
                    pass,
                    commonName,
                    organization,
                    organizationalUnit,
                    locality,
                    state,
                    country,
                    validity[0],
                    validity[1]
            );

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
