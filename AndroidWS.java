package Simulador;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;
import java.util.ResourceBundle;

import javax.imageio.ImageIO;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.StreamingOutput;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.chart.renderer.xy.XYDotRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;


import Simulador.db.BDConnection;
import Simulador.entitats.Alumno;
import Simulador.entitats.Animal;
import Simulador.entitats.EstadoGranja;
import Simulador.entitats.GranjaTerneros;
import Simulador.entitats.Ingrediente;
import Simulador.entitats.ListaEventos;
import Simulador.entitats.ListaIngredientes;
import Simulador.entitats.RacionLactantes;
import Simulador.entitats.RacionNeonatos;
import Simulador.entitats.Ternero;
import Simulador.entitats.Usuario;
import Simulador.entitats.Granja;
import Simulador.entitats.Vaca;

@Path("/android")
public class AndroidWS {

	@Path("/Login")	
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	public JSONObject Login(JSONObject json) throws JSONException {
		String user="";
		String pass="";
		JSONObject result= new JSONObject();
		user = json.get("username").toString();
		pass = json.get("pwd").toString();
		
		if (( user!= "") && ( pass!= "")) {
			//XIFRAR PASSWORD
			Usuario usuario = new Usuario (user, pass);
			
			//torno a construir amb l'ID ja que si no no tŽ nom
			
			usuario=new Usuario(usuario.getId());
			
			//Construeixo el JSON de resposta amb les dades que m'interessen de l'usuari
			result.put("rol", usuario.getRol());
			result.put("id", usuario.getId());
			result.put("nom", usuario.getNombre());
			
		}
		return result;
		
	}
	@Path("/Granja")	
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	public JSONObject Granja(JSONObject json) throws JsonGenerationException, JsonMappingException, IOException, JSONException {
		
		String id;
		Integer dia=-1;
		JSONObject result= new JSONObject();
		
		//Recupero l'ID de l'usuari loguejat
		id = json.get("id").toString();
		
		if (json.has("dia")){
		dia=Integer.parseInt(json.get("dia").toString());
		}

		if (dia >=0){
		EstadoGranja estadoInicial = new EstadoGranja(Integer.parseInt(id), dia);
	    ObjectMapper mapper = new ObjectMapper();
	    result =new JSONObject(mapper.writeValueAsString(estadoInicial));
		//Accedeixo a l'estat inicial de la granja de l'usuari i converteixo l'objecte a JSON
		}else{
			EstadoGranja estadoInicial = new EstadoGranja(Integer.parseInt(id));
		    ObjectMapper mapper = new ObjectMapper();
		    result =new JSONObject(mapper.writeValueAsString(estadoInicial));
			
		}
	    return result;
	}
	@Path("/GraficaGranja")	
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	public JSONArray GraficaGranja(JSONObject json) throws JsonGenerationException, JsonMappingException, IOException, JSONException {
	 
	 
		String id;
		JSONArray result= new JSONArray();

		
		id = json.get("id").toString();
		String consulta = "SELECT (dia - (edad - diasEnLactancia)) as diaLactacion, produccion, vaca "
		  + "FROM produccion, animales, vacas WHERE id = vaca AND vaca = idVaca and granja = " + id + " and "
		  + "dia > (edad - diasEnLactancia)  and diasEnLactancia > 0 and partos ";
		  
		consulta +=	"> 1 ORDER BY diaLactacion desc";

		BDConnection database = null;
		try {
			database = new BDConnection();
			ResultSet rs = database.executeQuery(consulta);
			while (rs.next()) {
				JSONObject series= new JSONObject();
				series.put("dia", rs.getInt("diaLactacion"));
				series.put("produccion", rs.getDouble("produccion"));
				result.put(series);
			
			}

		} catch (SQLException sqle) { 
		} finally { database.close(); } 
		System.out.println(result.toString());
		return result;
	}

	@Path("/Simular")	
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	public void Simular(JSONObject json) throws JsonGenerationException, JsonMappingException, IOException, JSONException {
		
	Integer id=	(Integer) json.get("id");
	Granja granja=new Granja(id);
	
	granja.simularDiaGranja();
	
	
	}
	@Path("/Vaques")	
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	public JSONArray Vaques(JSONObject json) throws JsonGenerationException, JsonMappingException, IOException, JSONException {
		
	//Recuperem la granja segons les dades que ens han passat
	Integer id=	(Integer) json.get("id");
	String ordre= (String) json.get("ordre");
	String direccio= (String) json.get("direccio");
	Integer grup=	Integer.parseInt(json.get("grup").toString());
	
	/* El que obtenim de la granja a vegades Žs l'identificador i no la descripci—. Per aix˜ carreguem l'arxiu de configuraci— segons
	 l'idioma per tal d'extreure les descripcions i ja passar-les al m˜bil
	 
	 OJO QUE ESTË POSAT CATALË HARDCODED, S'HA DE PERMETRE TRIAR L'IDIOMA DESDE LES PREFERƒNCIES DEL TELƒFON
	 
	 */
	
	//cada crida recollim nomŽs 10 vaques
	Integer primeraVaca= Integer.parseInt(json.get("primeraVaca").toString());

	Granja granja=new Granja(id,ordre,direccio,grup);
	
	Integer numeroVaques;

	
	
	JSONArray arrayVaques= new JSONArray();

	
	Integer mida= granja.getTamanyo();
	
	numeroVaques=Math.min(mida,primeraVaca+10);
	
	
	for (int i = primeraVaca; i < numeroVaques; i++) {
		Vaca vaca=granja.getVaca(i);
		
		
		JSONObject jsonVaca = vacaToJson(vaca,i,mida);
		
		arrayVaques.put(jsonVaca);
	}
	//System.out.println(arrayVaques.toString());
	return arrayVaques;
	}
	public JSONObject vacaToJson(Vaca vaca, int i, int mida) throws JSONException{
		Boolean ultima=false;
		JSONObject jsonVaca= new JSONObject();
		Locale local = new Locale("ca");
		ResourceBundle res = ResourceBundle.getBundle("MessageBundle", local);
		
		jsonVaca.put("idVaca",vaca.getId());
		
		/* 
		 Hem de treure la descripci— de l'estat segons l'idioma de l'arxiu de configuraci—
		 */
		if (vaca.getInseminada()>0){
		jsonVaca.put("estat",res.getString("estadoI"));
		} else{
		jsonVaca.put("estat",res.getString("estado" + vaca.getEstado()));
					
		}
		jsonVaca.put("IDestat",vaca.getEstado());	
		jsonVaca.put("prod",vaca.getProduccionRealHoy());
		jsonVaca.put("diesLact",vaca.getDiasEnLactancia());
		jsonVaca.put("diesCicle",vaca.getDiasDeCiclo());
		jsonVaca.put("diesGest",vaca.getDiasDeGestacion());
		jsonVaca.put("diesInsem",vaca.getEdad() - vaca.getInseminada());
		jsonVaca.put("parts",vaca.getPartos());		
		jsonVaca.put("proteina", vaca.getProteina());
		jsonVaca.put("greix",vaca.getGrasa());
		jsonVaca.put("ccs",vaca.getCelulasSomaticas());
		
		
		//mirem si hi ha la œltima vaca, per no tornar a demanar mŽs dades al servidor
		ultima=(i==mida-1);
		jsonVaca.put("ultima",ultima);
		return jsonVaca;
		}
	@Path("/Vedells")	
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	public JSONArray Vedells(JSONObject json) throws JsonGenerationException, JsonMappingException, IOException, JSONException {
		
	//Recuperem la granja segons les dades que ens han passat
	Integer id=	(Integer) json.get("id");
	String ordre= (String) json.get("ordre");
	String direccio= (String) json.get("direccio");
	Integer grup=	Integer.parseInt(json.get("grup").toString());
	
	
	
	//cada crida recollim nomŽs 10 vaques
	Integer primeraVaca= Integer.parseInt(json.get("primeraVaca").toString());
	GranjaTerneros granja=new GranjaTerneros(id,ordre,direccio,grup);
	Integer numeroVaques;
	Boolean ultima=false;	
	JSONArray arrayVaques= new JSONArray();
	Integer mida= granja.getTamanyo();
	numeroVaques=Math.min(mida,primeraVaca+10);
	
	for (int i = primeraVaca; i < numeroVaques; i++) {
		
		Ternero vaca=granja.getTernero(i);
		
		JSONObject jsonVaca= vedellToJson(vaca,i,mida);
		
		arrayVaques.put(jsonVaca);
		

		
	}
	System.out.println(arrayVaques.toString());
	return arrayVaques;
	}
	public JSONObject vedellToJson(Ternero vaca, int i, int mida) throws JSONException{
		Boolean ultima=false;
		JSONObject jsonVaca= new JSONObject();
		jsonVaca.put("idVedell",vaca.getId());
		/* El que obtenim de la granja a vegades Žs l'identificador i no la descripci—. Per aix˜ carreguem l'arxiu de configuraci— segons
		 l'idioma per tal d'extreure les descripcions i ja passar-les al m˜bil
		 
		 OJO QUE ESTË POSAT CATALË HARDCODED, S'HA DE PERMETRE TRIAR L'IDIOMA DESDE LES PREFERƒNCIES DEL TELƒFON
		 
		 */
		Locale local = new Locale("ca");
		ResourceBundle res = ResourceBundle.getBundle("MessageBundle", local);
		/* 
		 Hem de treure la descripci— de l'estat segons l'idioma de l'arxiu de configuraci—
		 */
		if (vaca.getInseminada()>0){
		jsonVaca.put("estat",res.getString("estadoI"));
		} else{
		jsonVaca.put("estat",res.getString("estado" + vaca.getEstado()));
					
		}
		jsonVaca.put("IDestat",vaca.getEstado());	
		jsonVaca.put("alsada",vaca.getAltura());	
		jsonVaca.put("pes",vaca.getPeso());	
		jsonVaca.put("diesCicle",vaca.getDiasDeCiclo());
		jsonVaca.put("diesGest",vaca.getDiasDeGestacion());
		jsonVaca.put("diesInsem",vaca.getEdad() - vaca.getInseminada());
		jsonVaca.put("edat",vaca.getEdad());	
		//mirem si hi ha la œltima vaca, per no tornar a demanar mŽs dades al servidor
		ultima=(i==mida-1);
		jsonVaca.put("ultima",ultima);	

		//aquesta part Žs per tornar els grups de la granja per a omplir dinamicament els spinners.De moment els valors son estˆtics
		/*ArrayList <Integer> grups=granja.getGrupos();
		jsonVaca.put("grups", grups);*/
		return jsonVaca;
	}
	@Path("/GraficaGranjaImatge")	
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces("image/png")
	
	public StreamingOutput GraficaGranjaImatge(JSONObject json) throws JsonGenerationException, JsonMappingException, IOException, JSONException {
		
		JFreeChart grafica = null;
		grafica = crearGraficaGranja(json.get("id").toString(), 0);
		
		final BufferedImage ima =grafica.createBufferedImage(570, 200);
		
		return new StreamingOutput() { 
	        @Override 
	        public void write(OutputStream output) throws IOException, 
	WebApplicationException { 
	            ImageIO.write(ima, "png", output); 
	        } 
	    }; 
	}
	
	@Path("/GraficaVacaImatge")	
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces("image/png")
	
	public StreamingOutput GraficaVacaImatge(JSONObject json) throws JsonGenerationException, JsonMappingException, IOException, JSONException {
		
		JFreeChart grafica = null;
		
		int dias = 365;
    	try {
    		dias = json.getInt("dies");
    	} catch (NumberFormatException nmfe) { dias=365; }
    	grafica = crearGraficaVaca(json.getString("idVaca"), dias);
		
		final BufferedImage ima =grafica.createBufferedImage(570, 200);
		
		return new StreamingOutput() { 
	        @Override 
	        public void write(OutputStream output) throws IOException, 
	WebApplicationException { 
	            ImageIO.write(ima, "png", output); 
	        } 
	    }; 
	}
	@Path("/AccionsVaca")	
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	
	public JSONObject AccionsVaca(JSONObject json) throws JsonGenerationException, JsonMappingException, IOException, JSONException {
		
		JSONObject ret = new JSONObject();
		Integer idVaca =Integer.parseInt(json.get("idVaca").toString());
		//Agafem la vaca en questi—
		Vaca vaca =new Vaca(idVaca);
		ret=vacaToJson(vaca,2,1);
		if (vaca.getEstado() > Vaca.MUERTA){
			if (!vaca.getAcciones().isInseminar()) {
				ret.put("Inseminar", true);
			}else{
				ret.put("Inseminar", false);
			}
			if (vaca.getSeca() == 0){
				if (!vaca.getAcciones().isSecar()) {
					ret.put("Secar", true);
				}else{
					ret.put("Secar", false);
				}
			}
			if ((vaca.getInseminada() != 0) && ((vaca.getEdad() - vaca.getInseminada()) > (vaca.getVariablesCurso().getGestacion().getDiasPendientesCiclo().getSuperior() + Vaca.MARGEN_PARA_DESCUBRIR_ESTADO))) {
				ret.put("Diagnostic", true);
			}
			if (Alumno.puedeSacrificar(vaca.getGranja())){
				ret.put("Sacrificar", true);
			}	
    		if (vaca.getGrupo() == 0) {
    			ret.put("Dieta", "A");
    		} else{
    			ret.put("Dieta", "B");
    		}
    		if (vaca.getEstado() == Vaca.CICLANDO) {
    			if ((vaca.getDiaFinalCiclo() - vaca.getDiasDeCiclo()) == 1) {
    				ret.put("Celo", true);
    			}
    			if (((vaca.getDiaFinalCiclo() - vaca.getDiasDeCiclo()) == 0) || ((vaca.getDiaFinalCiclo() - vaca.getDiasDeCiclo()) == 2)) {
    				ret.put("Precelo", true);
        			
    			}
    		}
		}else{
			ret.put("Morta", true);
		}
		return ret;	 
	}
	@Path("/HistoricVaca")	
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	
	public JSONArray HistoricVaca(JSONObject json) throws JsonGenerationException, JsonMappingException, IOException, JSONException {

		Locale local = new Locale("ca");
		ResourceBundle res = ResourceBundle.getBundle("MessageBundle", local);
		JSONArray arrayHistoric= new JSONArray();
		Integer idVaca =Integer.parseInt(json.get("idVaca").toString());
		//Agafem la vaca en questi—
		
		ListaEventos historia = new ListaEventos(idVaca);
		
		for (int i = 0; i < historia.size(); i++) {
			JSONObject ret = new JSONObject();

			Calendar dia = Calendar.getInstance(res.getLocale());
			dia.set(Calendar.DAY_OF_YEAR, 1 + historia.getDia(i));
			ret.put("dia",dia.get(Calendar.DAY_OF_MONTH));
			ret.put("mes", dia.get(Calendar.MONTH)+1);
			ret.put("any", dia.get(Calendar.YEAR));
			ret.put("event", res.getString("evento" + historia.getEvento(i)));
			
			arrayHistoric.put(ret);
		}
		
		return arrayHistoric;	 
	}
	@Path("/ModificarVaca")	
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	public JSONObject ModificarVaca(JSONObject json) throws JsonGenerationException, JsonMappingException, IOException, JSONException {
		
		JSONObject ret = new JSONObject();
		Integer idVaca =Integer.parseInt(json.get("idVaca").toString());
		//Agafem la vaca en questi—
		Vaca vaca =new Vaca(idVaca);
		
		if (json.get("accio").toString().equals("inseminar")){
			
			vaca.getAcciones().inseminar(json.getString("valor"), vaca.getId());

			
		}else if (json.get("accio").toString().equals("secar")){
			vaca.getAcciones().secar(json.getString("valor"), vaca.getId());
			
		}else if (json.get("accio").toString().equals("diagnostic")){
			vaca.descubrirEstado();
			
		}else if (json.get("accio").toString().equals("sacrificar")){
			vaca.sacrificar();
			
		}else if (json.get("accio").toString().equals("dieta")){
			Animal animal = new Animal(idVaca);
			animal.cambiarGrupo(Integer.parseInt(json.get("valor").toString()));
		}else {
			
		}
		ret=AccionsVaca(json);
		
		return ret;	 
	}	
	@Path("/ModificarVedell")	
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	public JSONObject ModificarVedell(JSONObject json) throws JsonGenerationException, JsonMappingException, IOException, JSONException {
		
		JSONObject ret = new JSONObject();
		Integer idVaca =Integer.parseInt(json.get("idVaca").toString());
		//Agafem la vaca en questi—
		Ternero vaca =new Ternero(idVaca);
		
		if (json.get("accio").toString().equals("inseminar")){
			
			vaca.getAcciones().inseminar(json.getString("valor"), vaca.getId());

			
		}else if (json.get("accio").toString().equals("crotalar")){
			vaca.getAcciones().crotalar(json.getString("valor"), vaca.getId());
			
		}else if (json.get("accio").toString().equals("desinfectar")){
			vaca.getAcciones().desinfectar(json.getString("valor"), vaca.getId());	
			
		}else if (json.get("accio").toString().equals("descubrir")){
			vaca.descubrirEstado();
		
		}else if (json.get("accio").toString().equals("sacrificar")){
			vaca.sacrificar();
			
		}else if (json.get("accio").toString().equals("destetar")){
			vaca.getAcciones().destetar(json.getString("valor"), vaca.getId());	
			
		}else {
			
		}
		ret=AccionsVaca(json);
		
		return ret;	 
	}	
	@Path("/AccionsVedell")	
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	
	public JSONObject AccionsVedell(JSONObject json) throws JsonGenerationException, JsonMappingException, IOException, JSONException {
		
		JSONObject ret = new JSONObject();
		Integer idVedell =Integer.parseInt(json.get("idVedell").toString());
		
		//Agafem el vedell
		
		Ternero vedell = new Ternero(idVedell);
		ret=vedellToJson(vedell,2,1);
		ret.put("estado", vedell.getEstado());
		
			/*Primero cogemos el grupo, si es un neonato o un lactante
			 *cogemos tanbiŽn la frecuencia y la cantidad
			 */
			ret.put("grupo", vedell.getGrupo());
			//Neonatos
    		if (vedell.getGrupo() == Ternero.GRUPO_NEONATOS) {
    			RacionNeonatos racion = new RacionNeonatos(vedell.getId());
    			ret.put("cantidad",racion.getCantidad());
    			ret.put("frecuencia", racion.getFrecuencia());
    		 //Lactantes	
    		}else if (vedell.getGrupo() == Ternero.GRUPO_LACTANTES){
    			RacionLactantes racion = new RacionLactantes(vedell.getId());
    			ret.put("cantidad",racion.getCantidad());
    			ret.put("frecuencia", racion.getFrecuencia());
    		}
    		/*
    		 * Ara continuem agafant les possibles accions que es poden realitzar sobre el vedell
    		 */
    	if (vedell.getEstado() > Ternero.MUERTA){
    		if (Alumno.puedeSacrificar(vedell.getGranja())){
				ret.put("Sacrificar", true);
			}
    		if(vedell.getEstado()<Ternero.LACTANTE){
    			if (!vedell.isCrotalado()) {
					if (!vedell.getAcciones().isCrotalar()) {
					ret.put("crotalar", true);
					}
    			}
				if (!vedell.isDesinfectado()) {
					if (!vedell.getAcciones().isDesinfectar()) {
						ret.put("desinfectar", true);
					}	
				}
    		}
			else {
				if (vedell.isHembra() && vedell.getEdad() > vedell.getVariablesCurso().getTernero().getDiasEstado(Ternero.CRECIMIENTO)) {
    				if (!vedell.getAcciones().isInseminar()) {
    					ret.put("inseminar", true);
    				}
    				
    				/*
    				 * Estas dos acciones tienen efecto inmediato sin esperar a la siguiente simulación
    				 */
    				if ((vedell.getInseminada() != 0) && ((vedell.getEdad() - vedell.getInseminada()) > (vedell.getVariablesCurso().getGestacion().getDiasPendientesCiclo().getSuperior() + Ternero.MARGEN_PARA_DESCUBRIR_ESTADO))) {
    					ret.put("descubrir", true);
    				}
				}

			}
			if (vedell.getEstado() < Ternero.CRECIMIENTO) {
				if (!vedell.getAcciones().isDestetar()) {
					ret.put("destetar", true);
				}
			
    		}
			if(vedell.isHembra()){
				ret.put("femella", true);
			}
			
    			
    	}else{
			ret.put("Morta", true);
		}
		return ret;	 
	}
	@Path("/Dieta")	
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	
	public JSONArray getDieta(JSONObject params) throws NumberFormatException, JSONException{
		JSONArray ret= new JSONArray();
		
		Integer granja =Integer.parseInt(params.get("IdGranja").toString());
		
		ListaIngredientes ingredientes = new ListaIngredientes(Granja.getCurso(granja));
		ArrayList <Integer> grupos = Granja.getGrupos(granja);
		for (int j = 0; j < grupos.size(); j++) {
			String nomdieta = "";
			if (grupos.get(j) == 0) {
				nomdieta = "A";
			} else {
				nomdieta = "B";
			}
			double totalProteina = 0;
			double totalGreix = 0;
			double totalCendres = 0;
			double totalFibra = 0;
			double totalCnf = 0;
			double totalEnergiaNeta = 0;
			double totalMateriaSeca = 0;
			double totalCantidad = 0;
			double totalPrecio = 0;
			int numeroVacas = Granja.getVacasGrupo(granja, grupos.get(j));
			for (int i = 0; i < ingredientes.size(); i++) {
				JSONObject json= new JSONObject();
				double cantidad = 0;
				cantidad = ingredientes.getIngrediente(i).getCantidad(granja, grupos.get(j)); 
				if ( cantidad > 0) {
					totalCantidad += cantidad;
					totalPrecio += ingredientes.getIngrediente(i).getPrecio() * cantidad;
					totalCnf += ingredientes.getIngrediente(i).getCnf() * cantidad;
					totalFibra += ingredientes.getIngrediente(i).getFibra() * cantidad;
					totalEnergiaNeta += ingredientes.getIngrediente(i).getEnergiaNeta() * cantidad;
					totalProteina += ingredientes.getIngrediente(i).getProteina() * cantidad;
					totalGreix += cantidad * ingredientes.getIngrediente(i).getGreix();
					totalCendres += cantidad * ingredientes.getIngrediente(i).getCendres();
					totalMateriaSeca += cantidad * ingredientes.getIngrediente(i).getMateriaSeca();
				}
				json.put("nomdieta", nomdieta);
				json.put("totpb",totalProteina);
				json.put("totee",totalGreix);
				json.put("totcen",totalCendres);
				json.put("totfnd",totalFibra);
				json.put("totcnf",totalCnf);
				json.put("totenl",totalEnergiaNeta);
				json.put("totms",totalMateriaSeca);
				json.put("toteukg",totalCantidad);
				json.put("totcant",totalPrecio);
				json.put("numvac",numeroVacas);
				
				
				json.put("nom",ingredientes.getIngrediente(i).getNombre());
				json.put("id",ingredientes.getIngrediente(i).getId());
				json.put("pb",ingredientes.getIngrediente(i).getProteina());
				json.put("ee",ingredientes.getIngrediente(i).getGreix());
				json.put("cen",ingredientes.getIngrediente(i).getCendres());
				json.put("fnd",ingredientes.getIngrediente(i).getFibra());
				json.put("cnf",ingredientes.getIngrediente(i).getCnf());
				json.put("enl",ingredientes.getIngrediente(i).getEnergiaNeta());
				json.put("ms",ingredientes.getIngrediente(i).getMateriaSeca());
				json.put("eukg",ingredientes.getIngrediente(i).getPrecio());
				json.put("cant",cantidad);
				json.put("grup", grupos.get(j));
				
				ret.put(json);
			}
		
		}
		
		return ret;
		
	}
	@Path("/modificarDieta")	
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	
	public void modificaDieta(JSONObject params) throws NumberFormatException, JSONException{
		
		Ingrediente ingrediente = new Ingrediente(Integer.parseInt(params.get("id").toString()));
		if (Utils.comprobaDouble(params.get("quantitat").toString())) {
			ingrediente.actualizaCantidadGranja(Utils.parseDouble(params.get("quantitat").toString()), Integer.parseInt(params.get("idUsuari").toString()), Integer.parseInt(params.get("grup").toString()));
		}		
	}
	
	
	public final JFreeChart crearGraficaGranja(final String idGranja, final int partos) {

		XYSeries series = new XYSeries("");
		String consulta = "SELECT (dia - (edad - diasEnLactancia)) as diaLactacion, produccion, vaca "
			  + "FROM produccion, animales, vacas WHERE id = vaca AND vaca = idVaca and granja = " + idGranja + " and "
			  + "dia > (edad - diasEnLactancia)  and diasEnLactancia > 0 and partos ";
		if (partos == 0) {
			consulta +=	"> 1 ORDER BY diaLactacion desc";
		} else {
			consulta += "= " + partos + " ORDER BY diaLactacion desc";
		}
		BDConnection database = null;
		try {
			database = new BDConnection();
			ResultSet rs = database.executeQuery(consulta);
			while (rs.next()) {
				series.add(rs.getInt("diaLactacion"), rs.getDouble("produccion"));
			}
		} catch (SQLException sqle) {
		} finally { database.close(); } 
        
		/*
		 * A partir de la serie genera el juego de datos que es necesario 
		 * para crear la gráfica. En este caso usamos una gráfica tipo ScatterPlot
		 * que vendría a ser una nube de puntos 
		 */
		XYSeriesCollection juegodatos = new XYSeriesCollection(series);
		JFreeChart chart = ChartFactory.createScatterPlot("", "Dia", "Produccio", juegodatos, PlotOrientation.VERTICAL, true, false, false);
		/*
		 * Configura el aspecto del gráfico con atributos y métodos del paquete JFreeChart. Como por ejemplo el tamaño
		 * de los puntos o su color
		 */
        XYPlot plot = (XYPlot) chart.getPlot();
        plot.setNoDataMessage("NO DATA");
        plot.setDomainZeroBaselineVisible(true);
        plot.setRangeZeroBaselineVisible(true);
        plot.setRenderer(new XYDotRenderer());
        XYDotRenderer renderer = (XYDotRenderer) plot.getRenderer();
        renderer.setSeriesOutlinePaint(0, Color.black);
        renderer.setAutoPopulateSeriesFillPaint(true);
        NumberAxis domainAxis = (NumberAxis) plot.getDomainAxis();
        domainAxis.setAutoRangeIncludesZero(false);
        domainAxis.setTickMarkInsideLength(2.0f);
        domainAxis.setTickMarkOutsideLength(0.0f);
        NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        rangeAxis.setTickMarkInsideLength(2.0f);
        rangeAxis.setTickMarkOutsideLength(0.0f);
        return chart;
	}
	public final JFreeChart crearGraficaVaca(final String idVaca, final int dias) {
		/*
		 * Crea un objeto para introducir la serie de pares de datos (coordenadas) 
		 * que sirven para generar la gráfica. Consulta a la base de datos por los datos
		 * de producción de la vaca de los últimos N días y los va añadiendo a la serie 
		 * de datos, asociando producciones a los días correspondientes  
		 */
		XYSeries series = new XYSeries("Producció de Llet");
		String consulta = "SELECT dia,produccion FROM produccion WHERE vaca='" + idVaca + "' ORDER BY dia desc LIMIT 1," + dias;
		BDConnection database = null;
		int diesMostrats = 0;

		try {
			database = new BDConnection();
			ResultSet rs = database.executeQuery(consulta);
			while (rs.next()) {
				series.add(rs.getInt("dia"), rs.getDouble("produccion"));
				diesMostrats++;
			}
			
		} catch (SQLException sqle) { 
		} finally { database.close(); } 
        
		XYSeriesCollection juegodatos = new XYSeriesCollection(series);
		juegodatos.setIntervalWidth(1.0);
		JFreeChart chart = ChartFactory.createHistogram("", Utils.desescaparHtml("&Uacute;ltims") + " " + diesMostrats + " " + Utils.desescaparHtml("dies"), Utils.desescaparHtml("Producci&oacute; (l)"), juegodatos , PlotOrientation.VERTICAL, false, false, false);
		chart.setBackgroundPaint(Color.WHITE);
		XYPlot plot = (XYPlot) chart.getPlot();
		final NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
		rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
		XYBarRenderer renderer = (XYBarRenderer) plot.getRenderer();
		renderer.setDrawBarOutline(false);
		GradientPaint colorear = new GradientPaint(0.0f, 0.0f, new Color(0, 100, 0), 0.0f, 0.0f, new Color(181, 215, 172));
		renderer.setSeriesPaint(0, colorear);
		return chart;
	}

	
}

