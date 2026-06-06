package com.example.notecortes.Cliente;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.notecortes.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
//La clase ClientPromotionsActivity gestiona el escaparate de ofertas activas para el usuario.
// Arquitectónicamente, aplica un flujo de ejecución condicional jerárquico: audita en primer lugar
// el documento del usuario en la colección users para comprobar si el flag de consentimiento
// notifications está activo. Solo si se cumple esta condición de privacidad, se dispara una consulta
// indexada con whereEqualTo en la colección promotions. Para el renderizado visual, se implementa
// un ArrayAdapter personalizado acoplado a un ListView dinámico para garantizar una interfaz corporativa
// y ligera
public class ClientPromotionsActivity extends AppCompatActivity {

    private ListView listViewPromos;
    private ArrayList<String> promoList;
    private ArrayAdapter<String> adapter;
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client_promotions);

        listViewPromos = findViewById(R.id.listViewPromos);
        promoList = new ArrayList<>();

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
//Nuevo
        adapter = new ArrayAdapter<>(
                this,
                //android.R.layout.simple_list_item_1,
                //promoList
                R.layout.item_promo,     // Quitamos el "android."
                R.id.textNombrePromo,
                promoList
        );

        listViewPromos.setAdapter(adapter);

        checkUserNotifications();
    }

    // --------------------------------------------------
    // COMPROBAR SI EL USUARIO TIENE NOTIFICATIONS = TRUE
    // --------------------------------------------------
    private void checkUserNotifications() {
        FirebaseUser user = auth.getCurrentUser();

        if (user == null) {
            Toast.makeText(this, "Usuario no autenticado", Toast.LENGTH_LONG).show();
            return;
        }

        db.collection("users")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        Boolean notifications = document.getBoolean("notifications");

                        if (notifications != null && notifications) {
                            loadPromotions();
                        } else {
                            Toast.makeText(
                                    this,
                                    "Las promociones están desactivadas para tu cuenta",
                                    Toast.LENGTH_LONG
                            ).show();
                        }
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(
                                this,
                                "Error verificando permisos",
                                Toast.LENGTH_LONG
                        ).show()
                );
    }

    // --------------------------------------------------
    // CARGAR SOLO LA DESCRIPCIÓN DE PROMOCIONES ACTIVAS
    // --------------------------------------------------
    private void loadPromotions() {
        db.collection("promotions")
                .whereEqualTo("activa", true)
                .get()
                .addOnSuccessListener(query -> {
                    promoList.clear();

                    for (DocumentSnapshot doc : query.getDocuments()) {
                        // Solo obtenemos la descripción
                        String descripcion = doc.getString("descripcion");

                        if (descripcion == null) {
                            descripcion = "Sin descripción";
                        }

                        // Añadimos solo el String de la descripción
                        promoList.add(descripcion);
                    }

                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(
                                this,
                                "Error cargando promociones",
                                Toast.LENGTH_LONG
                        ).show()
                );
    }
}