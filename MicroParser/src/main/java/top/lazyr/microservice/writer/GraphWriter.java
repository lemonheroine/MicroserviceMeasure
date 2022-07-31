package top.lazyr.microservice.writer;

import top.lazyr.constant.Printer;
import top.lazyr.microservice.graph.svc.FileNode;
import top.lazyr.microservice.graph.svc.InternalGraph;

import java.util.List;

/**
 * @author lazyr
 * @created 2022/4/22
 */
public class GraphWriter {
    public static void printInfo(InternalGraph graph) {
        List<FileNode> systemApiFiles = graph.filterSystemApiFiles();
        List<FileNode> systemCellFiles = graph.filterSystemCellFiles();
        List<FileNode> nonSystemCellFiles = graph.filterNonSystemCellFiles();
        List<FileNode> systemFeignFiles = graph.filterSystemFeignFiles();
        List<FileNode> nonSystemFeignFiles = graph.filterNonSystemFeignFiles();

        printFilesInfo(systemApiFiles, "systemApiFiles");
        printFilesInfo(systemCellFiles, "systemCellFiles");
        printFilesInfo(nonSystemCellFiles, "nonSystemCellFiles");
        printFilesInfo(systemFeignFiles, "systemFeignFiles");
        printFilesInfo(nonSystemFeignFiles, "nonSystemFeignFiles");

    }


    private static void printFilesInfo(List<FileNode> fileNodes, String title) {
        Printer.printTitle(title);
        if (fileNodes == null) {
            System.out.println("无");
            return;
        }
        for (FileNode fileNode : fileNodes) {
            System.out.println(fileNode);
        }
    }

}
