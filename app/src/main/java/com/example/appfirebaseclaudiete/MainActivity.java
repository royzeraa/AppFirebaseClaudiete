
package com.example.appfirebaseclaudiete;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private EditText txtCodigo, txtNombre, txtDueno, txtDireccion;
    private Spinner spMascota;
    private Button btnEnviar, btnCargar;
    private ListView lista;

    private FirebaseFirestore db;
    private ArrayList<String> mascotas;
    private ArrayAdapter<String> adaptador;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Conexión con Firebase
        db = FirebaseFirestore.getInstance();

        // Referencias a los elementos de la pantalla
        txtCodigo = findViewById(R.id.txtCodigo);
        txtNombre = findViewById(R.id.txtNombre);
        txtDueno = findViewById(R.id.txtDueno);
        txtDireccion = findViewById(R.id.txtDireccion);

        spMascota = findViewById(R.id.spMascota);
        btnEnviar = findViewById(R.id.btnEnviar);
        btnCargar = findViewById(R.id.btnCargar);
        lista = findViewById(R.id.lista);

        // Opciones del Spinner
        String[] tipos = {"Perro", "Gato", "Pájaro", "Otro"};

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                tipos
        );

        spinnerAdapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        spMascota.setAdapter(spinnerAdapter);

        // Adaptador de la lista
        mascotas = new ArrayList<>();

        adaptador = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                mascotas
        );

        lista.setAdapter(adaptador);

        // Botón para enviar datos
        btnEnviar.setOnClickListener(v -> guardarMascota());

        // Botón para cargar datos
        btnCargar.setOnClickListener(v -> cargarMascotas());

        // Cargar registros al iniciar
        cargarMascotas();
    }

    private void guardarMascota() {

        String codigo = txtCodigo.getText().toString().trim();
        String nombre = txtNombre.getText().toString().trim();
        String dueno = txtDueno.getText().toString().trim();
        String direccion = txtDireccion.getText().toString().trim();
        String tipo = spMascota.getSelectedItem().toString();

        // Validar campos
        if (codigo.isEmpty() || nombre.isEmpty()
                || dueno.isEmpty() || direccion.isEmpty()) {

            Toast.makeText(
                    this,
                    "Completa todos los campos",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        // Crear registro de mascota
        Map<String, Object> mascota = new HashMap<>();

        mascota.put("codigo", codigo);
        mascota.put("nombre", nombre);
        mascota.put("tipoMascota", tipo);
        mascota.put("dueño", dueno);
        mascota.put("direccion", direccion);

        // Guardar en Firestore
        db.collection("mascotas")
                .add(mascota)
                .addOnSuccessListener(documentReference -> {

                    Toast.makeText(
                            MainActivity.this,
                            "Mascota registrada correctamente",
                            Toast.LENGTH_SHORT
                    ).show();

                    limpiarCampos();
                    cargarMascotas();

                })
                .addOnFailureListener(e -> {

                    Toast.makeText(
                            MainActivity.this,
                            "Error al guardar: " + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();

                });
    }

    private void cargarMascotas() {

        db.collection("mascotas")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {

                    mascotas.clear();

                    for (DocumentSnapshot documento :
                            queryDocumentSnapshots.getDocuments()) {

                        String codigo = obtenerCampo(documento, "codigo");
                        String nombre = obtenerCampo(documento, "nombre");
                        String tipo = obtenerCampo(documento, "tipoMascota");
                        String dueno = obtenerCampo(documento, "dueño");
                        String direccion = obtenerCampo(documento, "direccion");

                        String registro =
                                "Código: " + codigo +
                                        "\nNombre: " + nombre +
                                        "\nTipo: " + tipo +
                                        "\nDueño: " + dueno +
                                        "\nDirección: " + direccion;

                        mascotas.add(registro);
                    }

                    adaptador.notifyDataSetChanged();

                    if (mascotas.isEmpty()) {
                        Toast.makeText(
                                MainActivity.this,
                                "No hay mascotas registradas",
                                Toast.LENGTH_SHORT
                        ).show();
                    }

                })
                .addOnFailureListener(e -> {

                    Toast.makeText(
                            MainActivity.this,
                            "Error al cargar: " + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();

                });
    }

    private String obtenerCampo(DocumentSnapshot documento, String campo) {

        Object valor = documento.get(campo);

        return valor != null ? valor.toString() : "";
    }

    private void limpiarCampos() {

        txtCodigo.setText("");
        txtNombre.setText("");
        txtDueno.setText("");
        txtDireccion.setText("");

        spMascota.setSelection(0);
    }
}
