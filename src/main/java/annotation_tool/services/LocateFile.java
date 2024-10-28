package annotation_tool.services;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javafx.fxml.FXML;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

public class LocateFile {

	public static String locateFolder() {
		DirectoryChooser directoryChooser = new DirectoryChooser();
		directoryChooser.setTitle("Selecione uma pasta");

		directoryChooser.setInitialDirectory(new File(System.getProperty("user.home")));

		File selectedDirectory = directoryChooser.showDialog(new Stage());

		if (selectedDirectory != null) {
			System.out.println("Diretório selecionado: " + selectedDirectory.getAbsolutePath());
			return selectedDirectory.getAbsolutePath();
		} else {
			System.out.println("Nenhuma pasta selecionada.");
			return null;
		}
	}

	public List<String> globsFolder(String path) {
		List<String> filesList = new ArrayList<>();

		// Verifica se o caminho é válido
		File directory = new File(path);
		if (directory.exists() && directory.isDirectory()) {
			// Lista todos os arquivos e diretórios no caminho fornecido
			File[] files = directory.listFiles();
			if (files != null) {
				for (File file : files) {
					filesList.add(file.getAbsolutePath());  // Adiciona o caminho absoluto
				}
			} else {
				System.out.println("Não foi possível listar os arquivos no diretório: " + path);
			}
		} else {
			System.out.println("O caminho fornecido não é um diretório válido: " + path);
		}

		return filesList;
	}

	// Método para listar imagens com uma extensão específica em um diretório
	public List<String> globsImages(String path) {
		List<String> imagesList = new ArrayList<>();

		// Verifica se o caminho é válido
		File directory = new File(path);
		if (directory.exists() && directory.isDirectory()) {
			// Lista todos os arquivos no caminho fornecido com a extensão especificada
			File[] files = directory.listFiles((dir, name) -> !name.toLowerCase().endsWith(".txt".toLowerCase()));
			if (files != null) {
				for (File file : files) {
					imagesList.add(file.getAbsolutePath());  // Adiciona o caminho absoluto do arquivo
				}
			} else {
				System.out.println("Não foi possível listar os arquivos no diretório: " + path);
			}
		} else {
			System.out.println("O caminho fornecido não é um diretório válido: " + path);
		}

		return imagesList;
	}
}

