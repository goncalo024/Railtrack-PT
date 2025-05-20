package com.example.guiacaminhosferro

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.view.Menu
import android.view.MenuItem
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

    companion object {
        private const val REQUEST_LOCATION = 1001
    }

    private lateinit var recyclerView: RecyclerView
    private lateinit var estacaoAdapter: EstacaoAdapter

    private val listaEstacoes = mutableListOf<Estacao>()
    private val listaEstacoesOrdenadas = mutableListOf<Estacao>()
    private val listaFiltrada = mutableListOf<Estacao>()

    private lateinit var database: DatabaseReference
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private var userLocation: Location? = null
    private var numeroEstacoesVisiveis = 10

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 1) RecyclerView + Adapter
        recyclerView = findViewById(R.id.recyclerViewEstacoes)
        recyclerView.layoutManager = LinearLayoutManager(this)
        estacaoAdapter = EstacaoAdapter(listaFiltrada) {
            carregarMaisEstacoes()
        }
        recyclerView.adapter = estacaoAdapter

        // 2) Toolbar + Drawer
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        val drawerLayout   = findViewById<DrawerLayout>(R.id.drawerLayout)
        val navigationView = findViewById<NavigationView>(R.id.navigationView)
        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()
        navigationView.setNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.nav_favoritos ->
                    startActivity(Intent(this, FavoritosActivity::class.java))
            }
            drawerLayout.closeDrawers()
            true
        }

        // 3) configuração de localização
        fusedLocationClient = LocationServices
            .getFusedLocationProviderClient(this)
        locationRequest = LocationRequest.create().apply {
            interval = 10_000L
            fastestInterval = 5_000L
            priority = Priority.PRIORITY_HIGH_ACCURACY
        }

        // 4) lê dados do Firebase
        database = FirebaseDatabase
            .getInstance()
            .reference
            .child("Estacoes")
        database.get().addOnSuccessListener { snap ->
            if (!snap.exists()) return@addOnSuccessListener

            listaEstacoes.clear()
            for (child in snap.children) {
                val est = child.getValue(Estacao::class.java) ?: continue
                if (est.id.isNullOrBlank()) est.id = child.key!!
                listaEstacoes.add(est)
            }
            // só ordena/exibe se já tivermos localização
            userLocation?.let { ordenarEstacoesPorProximidade() }
        }

        // 5) pede permissão & localização
        obterLocalizacao()
    }

    // busca permissão ou location
    private fun obterLocalizacao() {
        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_LOCATION
            )
            return
        }
        fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
            if (loc != null) {
                userLocation = loc
                ordenarEstacoesPorProximidade()
            } else {
                fusedLocationClient.requestLocationUpdates(
                    locationRequest, locationCallback, Looper.getMainLooper()
                )
            }
        }
    }

    // callback de location updates
    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            fusedLocationClient.removeLocationUpdates(this)
            userLocation = result.lastLocation
            ordenarEstacoesPorProximidade()
        }
    }

    // ========== Menu de pesquisa ==========
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        val item = menu.findItem(R.id.action_search)
        val sv   = item.actionView as SearchView
        sv.queryHint = "Pesquisar estação…"

        sv.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?) = false
            override fun onQueryTextChange(text: String?): Boolean {
                val q = text?.normalizeForSearch() ?: ""
                if (q.isEmpty()) {
                    // sem filtro: proximidade
                    ordenarEstacoesPorProximidade()
                } else {
                    // filtra em cima da lista já ordenada
                    val filtrada = listaEstacoesOrdenadas.filter { est ->
                        val nome = est.nome?.normalizeForSearch() ?: ""
                        val mor  = est.morada?.normalizeForSearch() ?: ""
                        nome.contains(q) || mor.contains(q)
                    }
                    listaFiltrada.clear()
                    listaFiltrada.addAll(filtrada)
                    estacaoAdapter.atualizarLista(listaFiltrada, mostrarBotao = false)
                }
                return true
            }
        })
        return true
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_LOCATION
            && grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED
        ) {
            obterLocalizacao()
        }
    }

    // ========= ordena e exibe proximidade =========
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
            // agora populamos a listaFiltrada com as primeiras N estações
            numeroEstacoesVisiveis = 10
            val iniciais = listaEstacoesOrdenadas.take(numeroEstacoesVisiveis)
            listaFiltrada.clear()
            listaFiltrada.addAll(iniciais)
            val botao = numeroEstacoesVisiveis < listaEstacoesOrdenadas.size
            estacaoAdapter.atualizarLista(listaFiltrada, botao)
        }
    }

    private fun carregarMaisEstacoes() {
        val total      = listaEstacoesOrdenadas.size
        val novoLimite = (numeroEstacoesVisiveis + 15).coerceAtMost(total)
        numeroEstacoesVisiveis = novoLimite

        val novaLista = listaEstacoesOrdenadas.take(numeroEstacoesVisiveis)
        listaFiltrada.clear()
        listaFiltrada.addAll(novaLista)
        val botao = numeroEstacoesVisiveis < total
        estacaoAdapter.atualizarLista(listaFiltrada, botao)
    }
}
