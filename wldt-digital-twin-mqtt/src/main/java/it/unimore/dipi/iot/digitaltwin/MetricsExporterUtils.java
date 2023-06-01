package it.unimore.dipi.iot.digitaltwin;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;

import java.io.File;
import java.io.FilenameFilter;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * @author Marco Picone, Ph.D. - picone.m@gmail.com
 * @project wldt-digital-twin-mqtt
 * @created 01/03/2022 - 15:45
 */
public class MetricsExporterUtils {

    public static final String METRICS_FOLDER = "metrics";

    public static void main(String[] args) {

        String[] metricsDirectories = loadAvailableMetricsFolder();
        System.out.println(Arrays.toString(metricsDirectories));

        if(metricsDirectories != null)
            for(String metricsDirectory : metricsDirectories) {
                String[] files = listAvailableFiles(Paths.get(METRICS_FOLDER, metricsDirectory).toString());
                System.out.println(Arrays.toString(files));

                if(files != null)
                    for(String fileName : files)
                        readCsvFile(Paths.get(METRICS_FOLDER, metricsDirectory, fileName).toString());
            }
    }

    public static List<Map<?, ?>> readCsvFile(String filePath){
        File input = new File(filePath);
        try {
            CsvSchema csv = CsvSchema.emptySchema().withHeader();
            CsvMapper csvMapper = new CsvMapper();
            MappingIterator<Map<?, ?>> mappingIterator =  csvMapper.reader().forType(Map.class).with(csv).readValues(input);
            List<Map<?, ?>> list = mappingIterator.readAll();
            return list;
        } catch(Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String[] listAvailableFiles(String metricsDirectory) {

        try{

            File metricsFolder = new File(metricsDirectory);
            if(metricsFolder.isDirectory()){

                String[] files = metricsFolder.list(new FilenameFilter() {
                    @Override
                    public boolean accept(File current, String name) {
                        return name.toLowerCase().endsWith(".csv");
                    }
                });

                return files;
            }
            else
                return null;

        }catch (Exception e){
            e.printStackTrace();
            return null;
        }

    }

    public static String[] loadAvailableMetricsFolder(){

        try{

            File metricsFolder = new File(METRICS_FOLDER);
            if(metricsFolder.isDirectory()){

                String[] directories = metricsFolder.list(new FilenameFilter() {
                    @Override
                    public boolean accept(File current, String name) {
                        return new File(current, name).isDirectory();
                    }
                });

                return directories;
            }
            else
                return null;

        }catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }

}
