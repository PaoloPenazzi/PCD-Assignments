package it.unibo.pcd.assignment.event.view;

import it.unibo.pcd.assignment.event.controller.ViewController;

import javax.swing.*;
import java.awt.*;

public class ViewFrame extends JFrame {
    private final ViewController controller;
    private JButton analyzeProjectButton;
    private JButton getPackageButton;
    private JButton getClassButton;
    private JButton getInterfaceButton;
    private JButton getProjectButton;
    private JButton startAnalysisButton;
    private JButton stopAnalysisButton;
    private JTextArea consoleTextArea;
    private JTextField packageCounterTextField;
    private JTextField classCounterTextField;
    private JTextField interfaceCounterTextField;
    private JLabel fileSelectedLabel;
    private JPanel northPanel;
    private JScrollPane centralPanel;
    private JPanel bottomPanel;

    public ViewFrame(ViewController controller) {
        this.controller = controller;
        this.setSize(1200, 620);
        this.setLocationRelativeTo(null);
        this.setTitle("Project Analyzer");
        this.setLayout(new BorderLayout());
        this.createAndAddPanels();
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setVisible(true);
    }

    private void createAndAddPanels() {
        this.createNorthPanel();
        this.createCentralPanel();
        this.createBottomPanel();
        this.getContentPane().add(BorderLayout.NORTH, this.northPanel);
        this.getContentPane().add(BorderLayout.CENTER, this.centralPanel);
        this.getContentPane().add(BorderLayout.SOUTH, this.bottomPanel);
    }

    private void createBottomPanel() {
        JPanel subPanelNorth = new JPanel(new GridLayout(3, 2));
        JPanel subPanelSouth = new JPanel();
        this.fileSelectedLabel = new JLabel("Ready");
        JLabel labelPackage = new JLabel("Package: ");
        this.packageCounterTextField = new JTextField();
        this.packageCounterTextField.setEditable(false);
        JLabel labelClass = new JLabel("Class: ");
        this.classCounterTextField = new JTextField();
        this.classCounterTextField.setEditable(false);
        JLabel labelInterface = new JLabel("Interface: ");
        this.interfaceCounterTextField = new JTextField();
        this.interfaceCounterTextField.setEditable(false);
        subPanelNorth.add(labelPackage);
        subPanelNorth.add(packageCounterTextField);
        subPanelNorth.add(labelClass);
        subPanelNorth.add(classCounterTextField);
        subPanelNorth.add(labelInterface);
        subPanelNorth.add(interfaceCounterTextField);
        subPanelSouth.add(this.fileSelectedLabel);
        this.bottomPanel = new JPanel(new BorderLayout());
        this.bottomPanel.add(BorderLayout.NORTH, subPanelNorth);
        this.bottomPanel.add(BorderLayout.SOUTH, subPanelSouth);
    }

    private void createNorthPanel() {
        this.northPanel = new JPanel();
        this.getClassButton = new JButton("Class Report");
        this.getClassButton.addActionListener(this.controller::analyzePressed);
        this.northPanel.add(this.getClassButton);
        this.getInterfaceButton = new JButton("Interface Report");
        this.getInterfaceButton.addActionListener(this.controller::analyzePressed);
        this.northPanel.add(this.getInterfaceButton);
        this.getPackageButton = new JButton("Package Report");
        this.getPackageButton.addActionListener(this.controller::analyzePressed);
        this.northPanel.add(this.getPackageButton);
        this.getProjectButton = new JButton("Project Report");
        this.getProjectButton.addActionListener(controller::analyzePressed);
        this.northPanel.add(this.getProjectButton);
        this.analyzeProjectButton = new JButton("Analyze Project");
        this.analyzeProjectButton.addActionListener(this.controller::analyzePressed);
        this.northPanel.add(this.analyzeProjectButton);
        this.startAnalysisButton = new JButton("Start");
        this.startAnalysisButton.addActionListener(this.controller::startAnalyses);
        this.northPanel.add(this.startAnalysisButton);
        this.stopAnalysisButton = new JButton("Stop");
        this.stopAnalysisButton.addActionListener(this.controller::stopAnalyses);
        this.northPanel.add(this.stopAnalysisButton);
    }

    private void createCentralPanel() {
        this.consoleTextArea = new JTextArea("");
        this.consoleTextArea.setLineWrap(true);
        this.consoleTextArea.setWrapStyleWord(true);
        this.consoleTextArea.setEditable(false);
        this.consoleTextArea.setSize(620, 300);
        this.centralPanel = new JScrollPane(this.consoleTextArea);
    }

    public JLabel getFileSelectedLabel() {
        return this.fileSelectedLabel;
    }

    public JTextArea getConsoleTextArea() {
        return this.consoleTextArea;
    }

    public JTextField getPackageCounterTextField() {
        return this.packageCounterTextField;
    }

    public JTextField getClassCounterTextField() {
        return this.classCounterTextField;
    }

    public JTextField getInterfaceCounterTextField() {
        return this.interfaceCounterTextField;
    }

    public ViewController getController() {
        return this.controller;
    }

    public JButton getAnalyzeProjectButton() {
        return this.analyzeProjectButton;
    }

    public JButton getGetPackageButton() {
        return this.getPackageButton;
    }

    public JButton getGetClassButton() {
        return this.getClassButton;
    }

    public JButton getGetInterfaceButton() {
        return this.getInterfaceButton;
    }

    public JButton getStartAnalysisButton() {
        return this.startAnalysisButton;
    }

    public JButton getStopAnalysisButton() {
        return this.stopAnalysisButton;
    }

    public JPanel getNorthPanel() {
        return this.northPanel;
    }

    public JScrollPane getCentralPanel() {
        return this.centralPanel;
    }

    public JPanel getBottomPanel() {
        return this.bottomPanel;
    }
}
