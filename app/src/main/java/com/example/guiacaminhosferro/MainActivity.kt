package com.example.guiacaminhosferro

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.Menu
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.location.*
import com.google.android.material.navigation.NavigationView
import com.google.firebase.database.*

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var estacaoAdapter: EstacaoAdapter
    private val listaEstacoes = mutableListOf<Estacao>()
    private val listaEstacoesOrdenadas = mutableListOf<Estacao>()
    private lateinit var database: DatabaseReference
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private var userLocation: Location? = null
    private var numeroEstacoesVisiveis = 10

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 1) configura RecyclerView
        recyclerView = findViewById(R.id.recyclerViewEstacoes)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // 2) cria o adapter VAZIO e amarra ao RecyclerView
        estacaoAdapter = EstacaoAdapter(listaEstacoes) {
            carregarMaisEstacoes()
        }
        recyclerView.adapter = estacaoAdapter

        // 3) prepara localização
        fusedLocationClient = LocationServices
            .getFusedLocationProviderClient(this)
        locationRequest = LocationRequest.create().apply {
            interval = 10000
            fastestInterval = 5000
            priority = Priority.PRIORITY_HIGH_ACCURACY
        }
        obterLocalizacao()

        // 4) busca Estações no Firebase
        database = FirebaseDatabase.getInstance().reference.child("Estacoes")
        database.get().addOnSuccessListener { snapshot ->
            if (!snapshot.exists()) return@addOnSuccessListener

            listaEstacoes.clear()
            for (estacaoSnap in snapshot.children) {
                val estacao = estacaoSnap.getValue(Estacao::class.java)
                if (estacao != null) {
                    // **Só** usar a key como id se o JSON NÃO nos tiver dado um id válido
                    if (estacao.id.isNullOrBlank()) {
                        estacao.id = estacaoSnap.key!!
                    }
                    listaEstacoes.add(estacao)
                }
            }

            if (userLocation != null) {
                ordenarEstacoesPorProximidade()
            } else {
                estacaoAdapter.atualizarLista(listaEstacoes, mostrarBotao = false)
            }
        }

        // 5) restante código de toolbar e menu…
        val drawerLayout   = findViewById<DrawerLayout>(R.id.drawerLayout)
        val navigationView = findViewById<NavigationView>(R.id.navigationView)
        val toolbar        = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar,
            R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        navigationView.setNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.nav_favoritos ->
                    startActivity(Intent(this, FavoritosActivity::class.java))
                // …
            }
            drawerLayout.closeDrawers()
            true
        }
    }

    // aqui vens com o resto: onCreateOptionsMenu, onRequestPermissionsResult…

    private fun obterLocalizacao() {
        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                1001
            )
            return
        }
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                userLocation = location
                ordenarEstacoesPorProximidade()
            } else {
                fusedLocationClient.requestLocationUpdates(
                    locationRequest, locationCallback, Looper.getMainLooper()
                )
            }
        }
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            super.onLocationResult(locationResult)
            fusedLocationClient.removeLocationUpdates(this)
            userLocation = locationResult.lastLocation
            ordenarEstacoesPorProximidade()
        }
    }

    private fun ordenarEstacoesPorProximidade() {
        userLocation?.let { loc ->
            listaEstacoesOrdenadas.clear()
            listaEstacoesOrdenadas.addAll(
                listaEstacoes.map { est ->
                    val lat = est.latitude?.toDoubleOrNull() ?: 0.0
                    val lon = est.longitude?.toDoubleOrNull() ?: 0.0
                    val dist = FloatArray(1).also {
                        Location.distanceBetween(
                            loc.latitude, loc.longitude, lat, lon, it
                        )
                    }[0]
                    est to dist
                }.sortedBy { it.second }
                    .map { it.first }
            )

            numeroEstacoesVisiveis = 10
            val iniciais = listaEstacoesOrdenadas.take(numeroEstacoesVisiveis)
            val botao    = numeroEstacoesVisiveis < listaEstacoesOrdenadas.size
            estacaoAdapter.atualizarLista(iniciais, botao)
        }
    }

    private fun carregarMaisEstacoes() {
        val total      = listaEstacoesOrdenadas.size
        val novoLimite = (numeroEstacoesVisiveis + 15).coerceAtMost(total)
        numeroEstacoesVisiveis = novoLimite

        val novaLista = listaEstacoesOrdenadas.take(numeroEstacoesVisiveis)
        val botao     = numeroEstacoesVisiveis < total
        estacaoAdapter.atualizarLista(novaLista, botao)
    }
}

