package com.example.practica_codigodebarras;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CAMERA = 111;
    private static final int REQUEST_GALLERY = 222;
    private static final int PERMISSION_REQUEST_CODE = 100;

    private Bitmap mSelectedImage;
    private ImageView mImageView;
    private TextView txtResults;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mImageView = findViewById(R.id.image_view);
        txtResults = findViewById(R.id.txtResults);

        if (!checkPermissions()) {
            requestPermissions();
        }
    }

    private boolean checkPermissions() {
        boolean cam = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;

        boolean readImages;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            readImages = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                    == PackageManager.PERMISSION_GRANTED;
        } else {
            readImages = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED;
        }
        return cam && readImages;
    }

    private void requestPermissions() {
        List<String> perms = new ArrayList<>();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            perms.add(Manifest.permission.CAMERA);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                perms.add(Manifest.permission.READ_MEDIA_IMAGES);
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                perms.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
        }
        if (!perms.isEmpty()) {
            ActivityCompat.requestPermissions(this, perms.toArray(new String[0]), PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (checkPermissions()) {
                Toast.makeText(this, "Permisos concedidos", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Los permisos son necesarios para usar la cámara y la galería", Toast.LENGTH_LONG).show();
            }
        }
    }

    public void abrirGaleria(View view) {
        if (!checkPermissions()) {
            requestPermissions();
            return;
        }
        Intent i = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(i, REQUEST_GALLERY);
    }

    public void abrirCamera(View view) {
        if (!checkPermissions()) {
            requestPermissions();
            return;
        }
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(intent, REQUEST_CAMERA);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null) {
            try {
                if (requestCode == REQUEST_CAMERA) {
                    if (data.getExtras() != null) {
                        Object extra = data.getExtras().get("data");
                        if (extra instanceof Bitmap) {
                            mSelectedImage = (Bitmap) extra;
                        }
                    }
                } else if (requestCode == REQUEST_GALLERY) {
                    if (data.getData() != null) {
                        mSelectedImage = MediaStore.Images.Media.getBitmap(getContentResolver(), data.getData());
                    }
                }
                if (mSelectedImage != null) {
                    mImageView.setImageBitmap(mSelectedImage);
                    txtResults.setText("Imagen lista para escanear.");
                } else {
                    txtResults.setText("No se pudo obtener la imagen.");
                }
            } catch (IOException e) {
                txtResults.setText("Error al leer la imagen: " + e.getMessage());
            }
        }
    }

    public void escanearBarras(View v) {
        if (mSelectedImage == null) {
            Toast.makeText(this, "Primero selecciona o toma una imagen", Toast.LENGTH_SHORT).show();
            return;
        }
        InputImage image = InputImage.fromBitmap(mSelectedImage, 0);

        BarcodeScannerOptions options = new BarcodeScannerOptions.Builder()
                .setBarcodeFormats(
                        Barcode.FORMAT_EAN_13,
                        Barcode.FORMAT_EAN_8,
                        Barcode.FORMAT_UPC_A,
                        Barcode.FORMAT_UPC_E,
                        Barcode.FORMAT_CODE_128,
                        Barcode.FORMAT_CODE_39,
                        Barcode.FORMAT_CODE_93,
                        Barcode.FORMAT_ITF,
                        Barcode.FORMAT_CODABAR
                )
                .build();

        BarcodeScanner scanner = BarcodeScanning.getClient(options);
        processScan(scanner, image);
    }

    public void escanearQR(View v) {
        if (mSelectedImage == null) {
            Toast.makeText(this, "Primero selecciona o toma una imagen", Toast.LENGTH_SHORT).show();
            return;
        }
        InputImage image = InputImage.fromBitmap(mSelectedImage, 0);

        BarcodeScannerOptions options = new BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                .build();

        BarcodeScanner scanner = BarcodeScanning.getClient(options);
        processScan(scanner, image);
    }

    private void processScan(BarcodeScanner scanner, InputImage image) {
        scanner.process(image)
                .addOnSuccessListener(barcodes -> {
                    if (barcodes == null || barcodes.isEmpty()) {
                        txtResults.setText("No se encontraron códigos.");
                        return;
                    }

                    Bitmap bitmap = mSelectedImage.copy(Bitmap.Config.ARGB_8888, true);
                    Canvas canvas = new Canvas(bitmap);
                    Paint paint = new Paint();
                    paint.setColor(Color.GREEN);
                    paint.setStrokeWidth(8);
                    paint.setStyle(Paint.Style.STROKE);

                    StringBuilder resultsText = new StringBuilder();
                    for (Barcode barcode : barcodes) {
                        Rect box = barcode.getBoundingBox();
                        if (box != null) canvas.drawRect(box, paint);

                        String display = barcode.getDisplayValue();
                        if (display == null) display = "";
                        resultsText.append("Código escaneado: ").append(display).append("\n");
                        resultsText.append("Formato: ").append(getBarcodeFormatName(barcode.getFormat())).append("\n");

                        switch (barcode.getValueType()) {
                            case Barcode.TYPE_WIFI: {
                                resultsText.append("Tipo: Wi-Fi\n");
                                if (barcode.getWifi() != null) {
                                    String ssid = barcode.getWifi().getSsid();
                                    String pwd = barcode.getWifi().getPassword();
                                    resultsText.append("SSID: ").append(ssid == null ? "" : ssid).append("\n");
                                    resultsText.append("Contraseña: ").append(pwd == null ? "" : pwd).append("\n");
                                    int sec = barcode.getWifi().getEncryptionType();
                                    String secName;
                                    switch (sec) {
                                        case Barcode.WiFi.TYPE_OPEN: secName = "Abierta"; break;
                                        case Barcode.WiFi.TYPE_WEP:  secName = "WEP";     break;
                                        case Barcode.WiFi.TYPE_WPA:  secName = "WPA/WPA2/WPA3"; break;
                                        default: secName = "Desconocida";
                                    }
                                    resultsText.append("Seguridad: ").append(secName).append("\n");
                                }
                                break;
                            }

                            case Barcode.TYPE_URL: {
                                resultsText.append("Tipo: URL\n");
                                if (barcode.getUrl() != null) {
                                    String title = barcode.getUrl().getTitle();
                                    String url = barcode.getUrl().getUrl();
                                    resultsText.append("Título: ").append(title == null ? "" : title).append("\n");
                                    resultsText.append("URL: ").append(url == null ? "" : url).append("\n");
                                }
                                break;
                            }

                            case Barcode.TYPE_GEO: {
                                resultsText.append("Tipo: Geolocalización\n");
                                if (barcode.getGeoPoint() != null) {
                                    resultsText.append("Latitud: ").append(barcode.getGeoPoint().getLat()).append("\n");
                                    resultsText.append("Longitud: ").append(barcode.getGeoPoint().getLng()).append("\n");
                                    resultsText.append("Mapa: https://maps.google.com/?q=")
                                            .append(barcode.getGeoPoint().getLat()).append(",")
                                            .append(barcode.getGeoPoint().getLng()).append("\n");
                                }
                                break;
                            }

                            case Barcode.TYPE_PRODUCT: {
                                resultsText.append("Tipo: Producto\n");
                                String val = barcode.getDisplayValue();
                                if (val == null || val.isEmpty()) val = barcode.getRawValue();
                                resultsText.append("Código de producto (GTIN/EAN/UPC): ")
                                        .append(val == null ? "" : val).append("\n");
                                break;
                            }

                            case Barcode.TYPE_CONTACT_INFO: {
                                resultsText.append("Tipo: Contacto\n");
                                if (barcode.getContactInfo() != null) {
                                    if (barcode.getContactInfo().getName() != null) {
                                        String name = barcode.getContactInfo().getName().getFormattedName();
                                        resultsText.append("Nombre: ").append(name == null ? "" : name).append("\n");
                                    }
                                    String org = barcode.getContactInfo().getOrganization();
                                    if (org != null && !org.isEmpty()) {
                                        resultsText.append("Organización: ").append(org).append("\n");
                                    }
                                    String title = barcode.getContactInfo().getTitle();
                                    if (title != null && !title.isEmpty()) {
                                        resultsText.append("Cargo: ").append(title).append("\n");
                                    }
                                    if (barcode.getContactInfo().getPhones() != null) {
                                        for (Barcode.Phone p : barcode.getContactInfo().getPhones()) {
                                            resultsText.append("Teléfono: ")
                                                    .append(p.getNumber() == null ? "" : p.getNumber()).append("\n");
                                        }
                                    }
                                    if (barcode.getContactInfo().getEmails() != null) {
                                        for (Barcode.Email em : barcode.getContactInfo().getEmails()) {
                                            resultsText.append("Email: ")
                                                    .append(em.getAddress() == null ? "" : em.getAddress()).append("\n");
                                        }
                                    }
                                }
                                break;
                            }

                            case Barcode.TYPE_EMAIL: {
                                resultsText.append("Tipo: Email\n");
                                if (barcode.getEmail() != null) {
                                    resultsText.append("Remitente: ")
                                            .append(barcode.getEmail().getAddress() == null ? "" : barcode.getEmail().getAddress())
                                            .append("\n");
                                    resultsText.append("Asunto: ")
                                            .append(barcode.getEmail().getSubject() == null ? "" : barcode.getEmail().getSubject())
                                            .append("\n");
                                    resultsText.append("Cuerpo: ")
                                            .append(barcode.getEmail().getBody() == null ? "" : barcode.getEmail().getBody())
                                            .append("\n");
                                }
                                break;
                            }

                            case Barcode.TYPE_PHONE: {
                                resultsText.append("Tipo: Teléfono\n");
                                if (barcode.getPhone() != null) {
                                    resultsText.append("Número: ")
                                            .append(barcode.getPhone().getNumber() == null ? "" : barcode.getPhone().getNumber())
                                            .append("\n");
                                }
                                break;
                            }

                            case Barcode.TYPE_SMS: {
                                resultsText.append("Tipo: SMS\n");
                                if (barcode.getSms() != null) {
                                    resultsText.append("Para: ")
                                            .append(barcode.getSms().getPhoneNumber() == null ? "" : barcode.getSms().getPhoneNumber())
                                            .append("\n");
                                    resultsText.append("Mensaje: ")
                                            .append(barcode.getSms().getMessage() == null ? "" : barcode.getSms().getMessage())
                                            .append("\n");
                                }
                                break;
                            }

                            case Barcode.TYPE_CALENDAR_EVENT: {
                                resultsText.append("Tipo: Evento de calendario\n");
                                if (barcode.getCalendarEvent() != null) {
                                    Barcode.CalendarEvent ev = barcode.getCalendarEvent();
                                    String sum = ev.getSummary();
                                    String loc = ev.getLocation();
                                    String desc = ev.getDescription();
                                    if (sum != null && !sum.isEmpty()) resultsText.append("Título: ").append(sum).append("\n");
                                    if (loc != null && !loc.isEmpty()) resultsText.append("Lugar: ").append(loc).append("\n");
                                    if (desc != null && !desc.isEmpty()) resultsText.append("Descripción: ").append(desc).append("\n");

                                    String startStr = formatCalendarDateTime(ev.getStart());
                                    if (!startStr.isEmpty()) resultsText.append("Inicio: ").append(startStr).append("\n");

                                    String endStr = formatCalendarDateTime(ev.getEnd());
                                    if (!endStr.isEmpty()) resultsText.append("Fin: ").append(endStr).append("\n");
                                }
                                break;
                            }

                            case Barcode.TYPE_TEXT: {
                                resultsText.append("Tipo: Texto\n");
                                String content = barcode.getDisplayValue();
                                if (content == null) content = "";
                                resultsText.append("Contenido: ").append(content).append("\n");
                                break;
                            }

                            case Barcode.TYPE_DRIVER_LICENSE: {
                                resultsText.append("Tipo: Licencia de conducir\n");
                                if (barcode.getDriverLicense() != null) {
                                    Barcode.DriverLicense dl = barcode.getDriverLicense();
                                    String first = dl.getFirstName();
                                    String last = dl.getLastName();
                                    if ((first != null && !first.isEmpty()) || (last != null && !last.isEmpty())) {
                                        resultsText.append("Nombre: ")
                                                .append(first == null ? "" : first).append(" ")
                                                .append(last == null ? "" : last).append("\n");
                                    }
                                    if (dl.getLicenseNumber() != null) resultsText.append("Número: ").append(dl.getLicenseNumber()).append("\n");
                                    if (dl.getBirthDate() != null)     resultsText.append("Nacimiento: ").append(dl.getBirthDate()).append("\n");
                                    if (dl.getIssueDate() != null)     resultsText.append("Emisión: ").append(dl.getIssueDate()).append("\n");
                                    if (dl.getExpiryDate() != null)    resultsText.append("Expiración: ").append(dl.getExpiryDate()).append("\n");
                                    if (dl.getAddressCity() != null || dl.getAddressState() != null || dl.getAddressZip() != null) {
                                        resultsText.append("Dirección: ")
                                                .append(dl.getAddressCity() == null ? "" : dl.getAddressCity()).append(", ")
                                                .append(dl.getAddressState() == null ? "" : dl.getAddressState()).append(" ")
                                                .append(dl.getAddressZip() == null ? "" : dl.getAddressZip()).append("\n");
                                    }
                                    if (dl.getIssuingCountry() != null) resultsText.append("País emisor: ").append(dl.getIssuingCountry()).append("\n");
                                }
                                break;
                            }

                            case Barcode.TYPE_ISBN: {
                                resultsText.append("Tipo: ISBN (Libro)\n");
                                String val = barcode.getDisplayValue();
                                if (val == null || val.isEmpty()) val = barcode.getRawValue();
                                resultsText.append("ISBN: ").append(val == null ? "" : val).append("\n");
                                break;
                            }

                            default: {
                                resultsText.append("Tipo: Genérico/Desconocido\n");
                                break;
                            }
                        }

                        resultsText.append("-----------------------------\n");
                    }

                    mImageView.setImageBitmap(bitmap);
                    txtResults.setText(resultsText.toString());
                })
                .addOnFailureListener(e -> {
                    txtResults.setText("Error al procesar la imagen: " + e.getMessage());
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private String formatCalendarDateTime(Barcode.CalendarDateTime dt) {
        if (dt == null) return "";
        int year = dt.getYear();
        int month = dt.getMonth();
        int day = dt.getDay();
        int hours = dt.getHours();
        int minutes = dt.getMinutes();

        StringBuilder sb = new StringBuilder();

        boolean hasDate = false;
        if (year > 0) { sb.append(year).append("-"); hasDate = true; }
        if (month > 0) { sb.append(String.format(Locale.getDefault(), "%02d-", month)); hasDate = true; }
        if (day > 0) { sb.append(String.format(Locale.getDefault(), "%02d", day)); hasDate = true; }

        if (hours > 0 || minutes > 0) {
            if (hasDate) sb.append(" ");
            sb.append(String.format(Locale.getDefault(), "%02d:%02d", hours, minutes));
        }

        return sb.toString().trim();
    }

    private String getBarcodeFormatName(int format) {
        switch (format) {
            case Barcode.FORMAT_QR_CODE: return "Código QR";
            case Barcode.FORMAT_AZTEC: return "Aztec";
            case Barcode.FORMAT_DATA_MATRIX: return "Data Matrix";
            case Barcode.FORMAT_PDF417: return "PDF417";
            case Barcode.FORMAT_EAN_13: return "EAN-13";
            case Barcode.FORMAT_EAN_8: return "EAN-8";
            case Barcode.FORMAT_UPC_A: return "UPC-A";
            case Barcode.FORMAT_UPC_E: return "UPC-E";
            case Barcode.FORMAT_CODE_128: return "Code 128";
            case Barcode.FORMAT_CODE_39: return "Code 39";
            case Barcode.FORMAT_CODE_93: return "Code 93";
            case Barcode.FORMAT_CODABAR: return "Codabar";
            case Barcode.FORMAT_ITF: return "ITF";
            default: return "Desconocido";
        }
    }
}
