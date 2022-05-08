package it.unibo.pcd.assignment.reactive.controller;

import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import it.unibo.pcd.assignment.reactive.model.ReactiveAnalyzerImpl;
import it.unibo.pcd.assignment.reactive.view.ViewFrame;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.event.ActionEvent;

public class ViewController {
    private final ViewFrame view;
    private final ReactiveAnalyzerImpl reactiveAnalyzerImpl;
    private boolean isStopped;
    Scheduler scheduler;
    Scheduler.Worker worker;

    public ViewController() {
        this.view = new ViewFrame(this);
        this.reactiveAnalyzerImpl = new ReactiveAnalyzerImpl();
        this.isStopped = false;
        this.scheduler = Schedulers.computation();
        this.createObservers();
    }

    public void analyzePressed(ActionEvent actionEvent) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setAcceptAllFileFilterUsed(false);
        JButton source = (JButton) actionEvent.getSource();
        switch (source.getText()) {
            case "Class Report": {
                this.reactiveAnalyzerImpl.setAnalysisType("class");
                FileFilter filter = new FileNameExtensionFilter("", "java");
                fileChooser.setFileFilter(filter);
                break;
            }
            case "Interface Report": {
                this.reactiveAnalyzerImpl.setAnalysisType("interface");
                FileFilter filter = new FileNameExtensionFilter("", "java");
                fileChooser.setFileFilter(filter);
                break;
            }
            case "Package Report": {
                this.reactiveAnalyzerImpl.setAnalysisType("package");
                fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                break;
            }
            case "Project Report": {
                this.reactiveAnalyzerImpl.setAnalysisType("project");
                fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                break;
            }
            case "Analyze Project": {
                this.reactiveAnalyzerImpl.setAnalysisType("analysis");
                fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                break;
            }
        }
        fileChooser.showSaveDialog(fileChooser);
        if (fileChooser.getSelectedFile() != null) {
            String path = fileChooser.getSelectedFile().getPath();
            this.reactiveAnalyzerImpl.setAnalysisPath(path);
            view.getFileSelectedLabel().setText(path);
        }
    }

    public void startPressed(ActionEvent actionEvent) {
        if (!this.reactiveAnalyzerImpl.getAnalysisPath().equals("")) {
            this.isStopped = false;
            this.clearOutput();
            this.worker = this.scheduler.createWorker();
            switch (this.reactiveAnalyzerImpl.getAnalysisType()) {
                case "class": {
                    this.worker.schedule(() -> {
                        this.reactiveAnalyzerImpl.getClassReport(this.reactiveAnalyzerImpl.getAnalysisPath());
                    });
                    break;
                }
                case "interface": {
                    this.worker.schedule(() -> {
                        this.reactiveAnalyzerImpl.getInterfaceReport(this.reactiveAnalyzerImpl.getAnalysisPath());
                    });
                    break;
                }
                case "package": {
                    this.worker.schedule(() -> {
                        this.reactiveAnalyzerImpl.getPackageReport(this.reactiveAnalyzerImpl.getAnalysisPath());
                    });
                    break;
                }
                case "project": {
                    this.worker.schedule(() -> {
                        this.reactiveAnalyzerImpl.getProjectReport(this.reactiveAnalyzerImpl.getAnalysisPath());
                    });
                    break;
                }
                case "analysis": {
                    this.worker.schedule(() -> {
                        this.reactiveAnalyzerImpl.analyzeProject(this.reactiveAnalyzerImpl.getAnalysisPath());
                    });
                    break;
                }
            }
        } else {
            JDialog dialog = new JDialog();
            dialog.add(new JLabel("Please select a file or a directory"));
            dialog.setSize(250, 100);
            dialog.setLocationRelativeTo(null);
            dialog.setVisible(true);
        }
    }

    public void stopPressed(ActionEvent actionEvent) {
        this.isStopped = true;
        this.worker.dispose();
    }

    private void createObservers() {
        this.setupInterfaceNumberObserver();
        this.setupClassNumberObserver();
        this.setupPackageNumberObserver();
        this.setupReportObserver();
    }

    private void clearOutput() {
        this.reactiveAnalyzerImpl.resetCounters();
        this.view.getConsoleTextArea().selectAll();
        this.view.getConsoleTextArea().replaceSelection("");
    }

    private void setupReportObserver() {
        Disposable reportObserver = this.reactiveAnalyzerImpl.getReportObservable()
                .subscribeOn(Schedulers.computation())
                .subscribe(res -> {
                    if (!this.isStopped) {
                        view.getConsoleTextArea().append(res.toString() + "\n\n");
                    }
                });
    }

    private void setupPackageNumberObserver() {
        Disposable packageObserver = this.reactiveAnalyzerImpl.getPackageNumberObservable()
                .subscribeOn(Schedulers.computation())
                .subscribe(res -> {
                    if (!this.isStopped) {
                        view.getPackageCounterTextField().setText(res + "");
                    }
                });
    }

    private void setupClassNumberObserver() {
        Disposable classObserver = reactiveAnalyzerImpl.getClassNumberObservable()
                .subscribeOn(Schedulers.computation())
                .subscribe(res -> {
                    if (!this.isStopped) {
                        view.getClassCounterTextField().setText(res + "");
                    }
                });
    }

    private void setupInterfaceNumberObserver() {
        Disposable interfaceObserver = this.reactiveAnalyzerImpl.getInterfaceNumberObservable()
                .subscribeOn(Schedulers.computation())
                .subscribe(res -> {
                    if (!this.isStopped) {
                        view.getInterfaceCounterTextField().setText(res + "");
                    }
                });
    }
}
