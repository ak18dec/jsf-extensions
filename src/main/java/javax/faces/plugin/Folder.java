package javax.faces.plugin;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


public class Folder {

	private File file;
	
	public Folder(File file) {
		
		this.file=file;
		
	}
	public Folder(String name) {
		
		file=new File(name);
		
	}
	
	public Folder(Folder parent,String name) {
		
		this.file=new File(parent+File.separator+name);
		
	}
	
	public Folder(String parent,String name) {
		
		this(new Folder(parent),name);
		
	}
	
	public List<Folder> getSubFolders() {
		
		List<Folder> folders=new ArrayList<Folder>();
		if(file.listFiles()!=null) {
			for(File file : this.file.listFiles()) {
					if(file.isDirectory())
						folders.add(new Folder(file));
			}
		}
		return folders;	
	}

	public Document getDocument(String resourceName,String library) {
		
		Document document;
		if(library!=null)
			document=new Document(file+File.separator+library+File.separator+resourceName);
		else
			document=new Document(file+File.separator+resourceName);
		return document.exists()?document:null;
			
	}
	
	public Document getDocument(String resourceName) {
		
		Document document=new Document(file+File.separator+resourceName);
		return document.exists()?document:null;
		
	}
	
	public Folder getParentFolder() {
		
		return new Folder(file.getParentFile().getAbsolutePath());
		
	}
	
	public String toString() {
		
		return file.toString();
		
	}
	
	public String getName() {
		
		return file.getName();
		
	}
}