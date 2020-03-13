package com.cores.springboot.backend.apirest.controllers;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.cores.springboot.backend.apirest.models.entity.Producto;
import com.cores.springboot.backend.apirest.models.services.IProductoService;

@CrossOrigin(origins= {"http://localhost:4200"})
@RestController
@RequestMapping("/api")
public class ProductoRestController {
	
	@Autowired
	private IProductoService productoService;
	
	private final Logger log=LoggerFactory.getLogger(ProductoRestController.class);
	
	@GetMapping("/productos")
	public List<Producto> index(){
		return productoService.findAll();
	}
	
	@GetMapping("/productos/page/{page}")
	public Page<Producto> index(@PathVariable Integer page){
		return productoService.findAll(PageRequest.of(page, 7));
	}
	
	@GetMapping("/productos/{id}")
	public Producto show(@PathVariable Long id) {
		return productoService.findById(id);
	}
	
	@PostMapping("/productos")
	@ResponseStatus(HttpStatus.CREATED)
	public Producto create(@RequestBody Producto producto) {
		return productoService.save(producto);
	}
	
	@PutMapping("/productos/{id}")
	@ResponseStatus(HttpStatus.CREATED)
	public Producto update(@RequestBody Producto producto,@PathVariable Long id) {
		Producto productoActual=productoService.findById(id);
		
		productoActual.setNombre(producto.getNombre());
		productoActual.setDescripcion(producto.getDescripcion());
		productoActual.setPeso(producto.getPeso());
		productoActual.setPrecio(producto.getPrecio());
		
		return productoService.save(productoActual);
	}
	
	@DeleteMapping("/productos/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@PathVariable Long id) {
		Producto producto=productoService.findById(id);
		String nombreFotoAnterior= producto.getFoto();
		
		if(nombreFotoAnterior !=null && nombreFotoAnterior.length()>0)
		{
			Path rutaFotoAnterior= Paths.get("uploads").resolve(nombreFotoAnterior).toAbsolutePath();
			File archivoFotoAnterior = rutaFotoAnterior.toFile();
			if(archivoFotoAnterior.exists()&&archivoFotoAnterior.canRead())
			{
				archivoFotoAnterior.delete();
			}
		}
		productoService.delete(id);		
	}
	
	@PostMapping("/productos/upload")
	public ResponseEntity<?> upload(@RequestParam("archivo") MultipartFile archivo, @RequestParam("id")Long id){
		Map<String, Object> response=new HashMap<>();
		
		Producto producto=productoService.findById(id);
		
		if(!archivo.isEmpty())
		{
			String nombreArchivo=UUID.randomUUID().toString()+"_"+archivo.getOriginalFilename().replace(" ","");
			Path rutaArchivo=Paths.get("uploads").resolve(nombreArchivo).toAbsolutePath();
			log.info(rutaArchivo.toString());
			
			try
			{
				Files.copy(archivo.getInputStream(), rutaArchivo);
			}
			catch(IOException e)
			{
				response.put("mensaje", "Error al subir la imagen: "+ nombreArchivo);
				response.put("error",e.getMessage().concat(": ").concat(e.getCause().getMessage()));
				return new ResponseEntity<Map<String,Object>>(response,HttpStatus.INTERNAL_SERVER_ERROR);
			}
			
			//revisamos si ya tiene foto para eliminarla
			String nombreFotoAnterior= producto.getFoto();
			
			if(nombreFotoAnterior !=null && nombreFotoAnterior.length()>0)
			{
				Path rutaFotoAnterior= Paths.get("uploads").resolve(nombreFotoAnterior).toAbsolutePath();
				File archivoFotoAnterior = rutaFotoAnterior.toFile();
				if(archivoFotoAnterior.exists()&&archivoFotoAnterior.canRead())
				{
					archivoFotoAnterior.delete();
				}
			}
			
			producto.setFoto(nombreArchivo);
			
			productoService.save(producto);
			
			response.put("producto", producto);
			response.put("mensaje", "La imagen "+ nombreArchivo +" se ha subido correctamente");
			
		}
		
		return new ResponseEntity<Map<String,Object>>(response,HttpStatus.CREATED);
	}
	
	@GetMapping("/uploads/img/{nombreFoto:.+}")
	public ResponseEntity<Resource> verFoto(@PathVariable String nombreFoto){
		
		Path rutaArchivo=Paths.get("uploads").resolve(nombreFoto).toAbsolutePath();
		log.info(rutaArchivo.toString());
		Resource recurso=null;
		
		try 
		{
			recurso= new UrlResource(rutaArchivo.toUri());
		}
		catch (MalformedURLException e)
		{
			e.printStackTrace();
		}
		
		if(!recurso.exists()&&!recurso.isReadable())
		{
			rutaArchivo=Paths.get("src/main/resources/static/images").resolve("no_image.png").toAbsolutePath();
			try 
			{
				recurso= new UrlResource(rutaArchivo.toUri());
			}
			catch (MalformedURLException e)
			{
				e.printStackTrace();
			}
			log.error("Error no se pudo cargar la imagen: "+nombreFoto);
		}
		HttpHeaders cabecera=new HttpHeaders();
		cabecera.add(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=\""+ recurso.getFilename()+"\"");
		
		return new ResponseEntity<Resource>(recurso, cabecera,HttpStatus.OK);
	}
}
