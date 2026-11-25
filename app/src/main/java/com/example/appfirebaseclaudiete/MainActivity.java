
package com.example.appfirebaseclaudiete;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    // ==========================================
    // ELEMENTOS DE LA INTERFAZ
    // ==========================================

    private EditText txtCodigo, txtNombre, txtDueno, txtDireccion;
    private Spinner spMascota;
    private Button btnEnviar, btnCargar;
    private ListView lista;

    // ==========================================
    // FIREBASE
    // ==========================================

    private FirebaseFirestore db;
    private ArrayList<String> mascotas;
    private ArrayAdapter<String> adaptador;

    // ==========================================
    // MQTT
    // ==========================================

    private static final String BROKER =
            "tcp://test.mosquitto.org:1883";

    private static final String TOPIC = "test/topic/claudio";

    private MqttAsyncClient mqttClient;

    // ==========================================
    // INICIO DE LA APLICACIÓN
    // ==========================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Inicializar Firebase
        db = FirebaseFirestore.getInstance();

        // Referencias de pantalla
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

        // Acciones de los botones
        btnEnviar.setOnClickListener(v -> guardarMascota());
        btnCargar.setOnClickListener(v -> cargarMascotas());

        // Cargar registros existentes
        cargarMascotas();

        // Conectar con MQTT
        conectarMQTT();
    }

    // ==========================================
    // CONEXIÓN MQTT
    // ==========================================

    private void conectarMQTT() {

        try {

            String clientId = "AppFirebaseClaudiete-" +
                    UUID.randomUUID().toString();

            mqttClient = new MqttAsyncClient(
                    BROKER,
                    clientId,
                    new MemoryPersistence()
            );

            MqttConnectOptions opciones = new MqttConnectOptions();

            opciones.setCleanSession(true);
            opciones.setAutomaticReconnect(true);
            opciones.setConnectionTimeout(30);
            opciones.setKeepAliveInterval(60);

            // Escuchar mensajes recibidos
            mqttClient.setCallback(new MqttCallback() {

                @Override
                public void connectionLost(Throwable cause) {

                    Log.e("MQTT", "Conexión perdida", cause);
                }

                @Override
                public void messageArrived(
                        String topic,
                        MqttMessage message
                ) {

                    String contenido = new String(
                            message.getPayload(),
                            StandardCharsets.UTF_8
                    );

                    Log.d("MQTT", "Topic: " + topic);
                    Log.d("MQTT", "Mensaje recibido: " + contenido);

                    runOnUiThread(() -> {

                        if (!isFinishing() && !isDestroyed()) {

                            Toast.makeText(
                                    MainActivity.this,
                                    "Mensaje MQTT: " + contenido,
                                    Toast.LENGTH_LONG
                            ).show();
                        }
                    });
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {

                    Log.d("MQTT", "Entrega completada");
                }
            });

            // Conectar al broker
            mqttClient.connect(
                    opciones,
                    null,
                    new IMqttActionListener() {

                        @Override
                        public void onSuccess(IMqttToken token) {

                            Log.d("MQTT", "Conectado al Broker");

                            runOnUiThread(() -> {

                                if (!isFinishing() && !isDestroyed()) {

                                    Toast.makeText(
                                            MainActivity.this,
                                            "Conectado al Broker MQTT",
                                            Toast.LENGTH_SHORT
                                    ).show();
                                }
                            });

                            suscribirTopic();
                        }

                        @Override
                        public void onFailure(
                                IMqttToken token,
                                Throwable exception
                        ) {

                            Log.e(
                                    "MQTT",
                                    "Error de conexión",
                                    exception
                            );

                            runOnUiThread(() -> {

                                if (!isFinishing() && !isDestroyed()) {

                                    Toast.makeText(
                                            MainActivity.this,
                                            "No se pudo conectar a MQTT",
                                            Toast.LENGTH_LONG
                                    ).show();
                                }
                            });
                        }
                    }
            );

        } catch (MqttException e) {

            Log.e("MQTT", "Error al iniciar MQTT", e);
        }
    }

    // ==========================================
    // SUSCRIPCIÓN AL TOPIC
    // ==========================================

    private void suscribirTopic() {

        try {

            if (mqttClient != null && mqttClient.isConnected()) {

                mqttClient.subscribe(
                        TOPIC,
                        0,
                        null,
                        new IMqttActionListener() {

                            @Override
                            public void onSuccess(IMqttToken token) {

                                Log.d(
                                        "MQTT",
                                        "Suscrito al Topic: " + TOPIC
                                );

                                runOnUiThread(() -> {

                                    if (!isFinishing() && !isDestroyed()) {

                                        Toast.makeText(
                                                MainActivity.this,
                                                "Suscripción MQTT exitosa",
                                                Toast.LENGTH_SHORT
                                        ).show();
                                    }
                                });
                            }

                            @Override
                            public void onFailure(
                                    IMqttToken token,
                                    Throwable exception
                            ) {

                                Log.e(
                                        "MQTT",
                                        "Error al suscribirse",
                                        exception
                                );
                            }
                        }
                );
            }

        } catch (MqttException e) {

            Log.e("MQTT", "Error en la suscripción", e);
        }
    }

    // ==========================================
    // PUBLICAR MASCOTA EN MQTT
    // ==========================================

    private void publicarMascotaMQTT(
            String codigo,
            String nombre,
            String tipo,
            String dueno,
            String direccion
    ) {

        try {

            if (mqttClient == null || !mqttClient.isConnected()) {

                Log.e("MQTT", "No hay conexión con el broker");

                Toast.makeText(
                        this,
                        "Mascota guardada, pero MQTT no está conectado",
                        Toast.LENGTH_LONG
                ).show();

                return;
            }

            // Crear objeto JSON con los datos
            JSONObject datos = new JSONObject();

            datos.put("codigo", codigo);
            datos.put("nombre", nombre);
            datos.put("tipoMascota", tipo);
            datos.put("dueno", dueno);
            datos.put("direccion", direccion);

            String contenido = datos.toString();

            // Crear mensaje MQTT
            MqttMessage mensaje = new MqttMessage(
                    contenido.getBytes(StandardCharsets.UTF_8)
            );

            mensaje.setQos(0);
            mensaje.setRetained(false);

            // Publicar mensaje
            mqttClient.publish(
                    TOPIC,
                    mensaje,
                    null,
                    new IMqttActionListener() {

                        @Override
                        public void onSuccess(IMqttToken token) {

                            Log.d(
                                    "MQTT",
                                    "Mascota publicada correctamente: " + contenido
                            );

                            runOnUiThread(() -> {

                                if (!isFinishing() && !isDestroyed()) {

                                    Toast.makeText(
                                            MainActivity.this,
                                            "Mascota enviada a MQTT correctamente",
                                            Toast.LENGTH_SHORT
                                    ).show();
                                }
                            });
                        }

                        @Override
                        public void onFailure(
                                IMqttToken token,
                                Throwable exception
                        ) {

                            Log.e(
                                    "MQTT",
                                    "Error al publicar mascota",
                                    exception
                            );

                            runOnUiThread(() -> {

                                if (!isFinishing() && !isDestroyed()) {

                                    Toast.makeText(
                                            MainActivity.this,
                                            "Error al enviar datos a MQTT",
                                            Toast.LENGTH_LONG
                                    ).show();
                                }
                            });
                        }
                    }
            );

        } catch (Exception e) {

            Log.e("MQTT", "Error preparando mensaje", e);
        }
    }

    // ==========================================
    // GUARDAR MASCOTA EN FIREBASE
    // ==========================================

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

        // Crear registro
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

                    // Publicar los mismos datos en MQTT
                    publicarMascotaMQTT(
                            codigo,
                            nombre,
                            tipo,
                            dueno,
                            direccion
                    );

                    // Actualizar pantalla
                    limpiarCampos();
                    cargarMascotas();

                })
                .addOnFailureListener(e -> {

                    Toast.makeText(
                            MainActivity.this,
                            "Error al guardar: " + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();

                    Log.e("FIREBASE", "Error al guardar mascota", e);
                });
    }

    // ==========================================
    // CARGAR MASCOTAS DESDE FIREBASE
    // ==========================================

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

                    Log.e("FIREBASE", "Error al cargar mascotas", e);
                });
    }

    // ==========================================
    // OBTENER CAMPO DE FIREBASE
    // ==========================================

    private String obtenerCampo(
            DocumentSnapshot documento,
            String campo
    ) {

        Object valor = documento.get(campo);

        return valor != null ? valor.toString() : "";
    }

    // ==========================================
    // LIMPIAR CAMPOS
    // ==========================================

    private void limpiarCampos() {

        txtCodigo.setText("");
        txtNombre.setText("");
        txtDueno.setText("");
        txtDireccion.setText("");

        spMascota.setSelection(0);
    }

    // ==========================================
    // CERRAR CONEXIÓN MQTT
    // ==========================================

    @Override
    protected void onDestroy() {

        if (mqttClient != null) {

            try {

                if (mqttClient.isConnected()) {

                    mqttClient.disconnectForcibly(0, 1000);
                }

                mqttClient.close();

            } catch (MqttException e) {

                Log.e(
                        "MQTT",
                        "Error al cerrar conexión",
                        e
                );
            }
        }

        super.onDestroy();
    }
}
