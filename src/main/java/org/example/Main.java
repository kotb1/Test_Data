package org.example;

import org.example.Creations.Create;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.util.Scanner;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    public static void main(String[] args) throws Exception {

        Scanner scanner = new Scanner(System.in);

        System.out.print("Enter IP: ");
        String ip = scanner.nextLine().trim();

        System.out.print("Enter Port: ");
        String port = scanner.nextLine().trim();

        int numberOfOrders;
        while (true) {
            System.out.print("Enter number of orders: ");
            try {
                numberOfOrders = Integer.parseInt(scanner.nextLine().trim());
                if (numberOfOrders > 0) break;
            } catch (NumberFormatException ignored) {
            }
            System.out.println("Please enter a positive whole number.");
        }

        Create run = new Create(ip, port);
        run.genrate_work_orders(numberOfOrders);

    }
}