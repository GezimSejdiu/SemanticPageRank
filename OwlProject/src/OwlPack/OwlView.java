/*
 * OwlView.java
 */
package OwlPack;

import com.hp.hpl.jena.graph.Capabilities;
import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.reasoner.InfGraph;
import com.hp.hpl.jena.reasoner.Reasoner;
import com.hp.hpl.jena.reasoner.ReasonerException;
import com.hp.hpl.jena.util.FileUtils;
import edu.stanford.smi.protege.exception.OntologyLoadException;
import edu.stanford.smi.protege.util.URIUtilities;
import edu.stanford.smi.protegex.owl.ProtegeOWL;
import edu.stanford.smi.protegex.owl.jena.JenaOWLModel;
import edu.stanford.smi.protegex.owl.model.OWLIndividual;
import edu.stanford.smi.protegex.owl.model.util.ImportHelper;
import edu.stanford.smi.protegex.owl.repository.impl.LocalFolderRepository;
import edu.stanford.smi.protegex.owl.swrl.SWRLRuleEngine;
import edu.stanford.smi.protegex.owl.swrl.SWRLRuleEngineFactory;
import edu.stanford.smi.protegex.owl.swrl.exceptions.SWRLFactoryException;
import edu.stanford.smi.protegex.owl.swrl.exceptions.SWRLRuleEngineException;
import edu.stanford.smi.protegex.owl.swrl.model.SWRLFactory;
import edu.stanford.smi.protegex.owl.swrl.model.SWRLImp;
import edu.stanford.smi.protegex.owl.swrl.parser.SWRLParseException;
import edu.stanford.smi.protegex.owl.swrl.sqwrl.*;
import edu.stanford.smi.protegex.owl.swrl.sqwrl.exceptions.SQWRLException;
import java.awt.Color;
import java.net.URISyntaxException;
import org.jdesktop.application.Action;
import org.jdesktop.application.ResourceMap;
import org.jdesktop.application.SingleFrameApplication;
import org.jdesktop.application.FrameView;
import org.jdesktop.application.TaskMonitor;
import java.awt.event.*;
import java.beans.Statement;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.swing.*;
import javax.swing.Timer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import org.jdesktop.application.Resource;
import org.jdesktop.application.SessionStorage.Property;

/**
 * The application's main frame.
 */
public class OwlView extends FrameView {

    public OwlView(SingleFrameApplication app) {
        super(app);

        initComponents();

        // status bar initialization - message timeout, idle icon and busy animation, etc
        ResourceMap resourceMap = getResourceMap();
        int messageTimeout = resourceMap.getInteger("StatusBar.messageTimeout");
        messageTimer = new Timer(messageTimeout, new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                statusMessageLabel.setText("");
            }
        });
        messageTimer.setRepeats(false);
        int busyAnimationRate = resourceMap.getInteger("StatusBar.busyAnimationRate");
        for (int i = 0; i < busyIcons.length; i++) {
            busyIcons[i] = resourceMap.getIcon("StatusBar.busyIcons[" + i + "]");
        }
        busyIconTimer = new Timer(busyAnimationRate, new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                busyIconIndex = (busyIconIndex + 1) % busyIcons.length;
                statusAnimationLabel.setIcon(busyIcons[busyIconIndex]);
            }
        });
        idleIcon = resourceMap.getIcon("StatusBar.idleIcon");
        statusAnimationLabel.setIcon(idleIcon);
        progressBar.setVisible(false);

        // connecting action tasks to status bar via TaskMonitor
        TaskMonitor taskMonitor = new TaskMonitor(getApplication().getContext());
        taskMonitor.addPropertyChangeListener(new java.beans.PropertyChangeListener() {

            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                String propertyName = evt.getPropertyName();
                if ("started".equals(propertyName)) {
                    if (!busyIconTimer.isRunning()) {
                        statusAnimationLabel.setIcon(busyIcons[0]);
                        busyIconIndex = 0;
                        busyIconTimer.start();
                    }
                    progressBar.setVisible(true);
                    progressBar.setIndeterminate(true);
                } else if ("done".equals(propertyName)) {
                    busyIconTimer.stop();
                    statusAnimationLabel.setIcon(idleIcon);
                    progressBar.setVisible(false);
                    progressBar.setValue(0);
                } else if ("message".equals(propertyName)) {
                    String text = (String) (evt.getNewValue());
                    statusMessageLabel.setText((text == null) ? "" : text);
                    messageTimer.restart();
                } else if ("progress".equals(propertyName)) {
                    int value = (Integer) (evt.getNewValue());
                    progressBar.setVisible(true);
                    progressBar.setIndeterminate(false);
                    progressBar.setValue(value);
                }
            }
        });

        model = (DefaultTableModel) OwlView.jTableResult.getModel();
        ONTOLOGY_URL = ".../OwlProject/SemanticSearchOnto-v.1.0.owl";
        Repository_Url = ".../OwlProject/Replace_New_Version/";
    }

    @Action
    public void showAboutBox() {
        if (aboutBox == null) {
            JFrame mainFrame = Owl.getApplication().getMainFrame();
            aboutBox = new OwlAboutBox(mainFrame);
            aboutBox.setLocationRelativeTo(mainFrame);
        }
        Owl.getApplication().show(aboutBox);
    }

    private void ClearModel() {
        int rows = model.getRowCount();
        try {
            for (int i = 0; i < rows; i++) {
                model.removeRow(i);
            }
        } catch (Exception ex) {
            ClearModel();
        }
        int columns = model.getColumnCount();
        try {
            for (int i = 1; i < columns; i++) {
                jTableResult.removeColumn(new TableColumn(i));
            }
            model = (DefaultTableModel) OwlView.jTableResult.getModel();
        } catch (Exception ex) {
            ClearModel();
        }
    }

    private void Replace(String toReplace) throws URISyntaxException {

        URI outputURI = new URI(("file:///" + ONTOLOGY_URL.replaceAll(" ", "%20")));

        File f = new File(outputURI);

        FileInputStream fs = null;
        InputStreamReader in = null;
        BufferedReader br = null;

        StringBuffer sb = new StringBuffer();

        String textinLine;

        try {
            fs = new FileInputStream(f);
            in = new InputStreamReader(fs);
            br = new BufferedReader(in);
            Boolean replacing = false;
            while (true) {
                textinLine = br.readLine();
                if (textinLine == null) {
                    break;
                } else if (textinLine.contains("<" + toReplace)) {
                    replacing = true;
                } else if (replacing) {
                    if (textinLine.contains("</" + toReplace + ">")) {
                        replacing = false;
                        if (textinLine.contains("</rdf:Description>")) {
                            sb.append("\n </rdf:Description>");
                        }
                    }
                } else {
                    sb.append(textinLine + "\n");
                }
            }

            fs.close();
            in.close();
            br.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            FileWriter fstream = new FileWriter(f);
            BufferedWriter outobj = new BufferedWriter(fstream);
            outobj.write(sb.toString());
            outobj.close();

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    public void ExecuteRule() throws SQWRLException, SWRLRuleEngineException, InterruptedException, SWRLFactoryException, URISyntaxException {

        jTextStatus.setBackground(Color.white);

        try {

            URI outputURI = new URI(("file:/" + ONTOLOGY_URL.replaceAll(" ", "%20")));
            JenaOWLModel owlModel = ProtegeOWL.createJenaOWLModelFromURI(outputURI.toString());
            outputURI = new URI(("file:/" + Repository_Url.replaceAll(" ", "%20")));
            LocalFolderRepository rep = new LocalFolderRepository(new File(Repository_Url), true);
            owlModel.getRepositoryManager().addGlobalRepository(rep);

            String status = " Ontology Loaded";

            SWRLFactory factory = new SWRLFactory(owlModel);
            SWRLRuleEngine ruleEngine = SWRLRuleEngineFactory.create(owlModel);

            try {

                jTextStatus.setText(status);

                SQWRLQueryEngine queryEngine;
                SQWRLResult result;
                queryEngine = SQWRLQueryEngineFactory.create(owlModel);

                String rule = "";
                rule = jTextRule.getText();

                queryEngine.createSQWRLQuery("Querys4", rule);
                result = queryEngine.runSQWRLQuery("Querys4");

                boolean columnsGenerated = false;
                int length = 1;
                int columnsCount = model.getColumnCount();
                while (result.hasNext()) {
                    List<SQWRLResultValue> r = result.getRow();
                    String[] individual = pattern.split(r.toString());

                    if (!columnsGenerated) {
                        length = individual.length;
                        if (columnsCount < length) {
                            for (int i = 1; i < length; i++) {
                                model.addColumn("Result" + i);
                            }
                        }
                        columnsGenerated = true;
                    }

                    Object[] row = new Object[length];

                    for (int i = 0; i < length; i++) {
                        if (i == 0) {
                            row[i] = individual[i].substring(1);
                        } else if (i == length - 1) {
                            row[i] = individual[i].substring(0, individual[1].length() - 1);
                        } else {
                            row[i] = individual[i];
                        }
                    }

                    model.addRow(row);
                    result.next();
                }

                status = jTextStatus.getText() + "\n" + " Finished";
                jTextStatus.setText(status);

            } catch (SWRLParseException ex) {
                jTextStatus.setText(" Rule isn't defined in proper way");
                jTextStatus.setBackground(Color.ORANGE);
            }
        } catch (OntologyLoadException ex) {
            jTextStatus.setText(" Ontology couldn't found. Check Path.");
            jTextStatus.setBackground(Color.ORANGE);
        }

    }

    public void ExecutePR() throws SQWRLException, SWRLRuleEngineException, InterruptedException, SWRLFactoryException, URISyntaxException {

      jTextStatus.setBackground(Color.white);

        int iterations = (Integer) spinnerIterations.getValue();

        if (iterations > 1) {
            iterations = iterations - 2;
        }
        if (iterations % 2 == 0) {
            iterations = iterations + 1;
        }
        try {

            try {

                URI outputURI = new URI(("file:/" + ONTOLOGY_URL.replaceAll(" ", "%20")));
                JenaOWLModel owlModel = ProtegeOWL.createJenaOWLModelFromURI(outputURI.toString());
                outputURI = new URI(("file:/" + Repository_Url.replaceAll(" ", "%20")));
                LocalFolderRepository rep = new LocalFolderRepository(new File(outputURI), true);
                owlModel.getRepositoryManager().addGlobalRepository(rep);
                outputURI = new URI(("file:/" + ONTOLOGY_URL.replaceAll(" ", "%20")));
                SWRLFactory factory = new SWRLFactory(owlModel);
                SWRLRuleEngine ruleEngine = SWRLRuleEngineFactory.create(owlModel);

                String status = "Ontology Loaded";
                
                factory.deleteImps();

                SWRLImp mainRule;
                mainRule = factory.createImp("hasOutLink(?wj, ?wrel) ∧ wiki:hasOutLinkValue(?wrel, ?wi) ∧ hasTotalNrOutLinks(?wj, ?cj) ∧ totalNrWeb-Sites(Constant1, ?n) ∧ swrlm:eval(?yj, \"1/cj\", ?cj) ˚ sqwrl:makeBag(?s, ?yj) ∧ sqwrl:groupBy(?s, ?wi) ˚ sqwrl:sum(?ally, ?s) ∧ swrlm:eval(?pri, \"(0.15 / n) + 0.85 * ally\", ?n, ?ally) → hasWebPR(?wi, ?pri)");
                ruleEngine.infer();

                factory.deleteImps();
                mainRule = factory.createImp("hasWebPR(?wj, ?prj) ∧ hasOutLink(?wj, ?wrel) ∧ wiki:hasOutLinkValue(?wrel, ?wi) ∧ hasTotalNrOutLinks(?wj, ?cj) ∧ totalNrWeb-Sites(Constant1, ?n) ∧ swrlm:eval(?yj, \"prj/cj\", ?prj, ?cj) ˚ sqwrl:makeBag(?s, ?yj) ∧ sqwrl:groupBy(?s, ?wi) ˚ sqwrl:sum(?ally, ?s) ∧ swrlm:eval(?pri, \"0.15 / n + 0.85 * ally\", ?n, ?ally) → hasWebAlly(?wi, ?pri)");
                ruleEngine.infer();

                System.out.println("Saving...");
                Collection errors;

                errors = new ArrayList();

                owlModel.save(new File("SemanticSearchOnto-v.1.0.owl").toURI(), FileUtils.langXMLAbbrev, errors);
                System.out.println("File saved with " + errors.size() + " errors.");
                int i = 0;
                while (i < iterations) {

                    if (i % 2 == 0) {
                        Replace("hasWebPR");
                    } else {
                        Replace("hasWebAlly");
                    }
                    System.out.println("Iteracion" + (i + 3));

                    owlModel.dispose();
                    mainRule = null;
                    owlModel = null;
                    factory = null;
                    ruleEngine = null;

                    System.gc();
                    java.lang.Runtime.getRuntime().freeMemory();
                    java.lang.Runtime.getRuntime().gc();

                    owlModel = ProtegeOWL.createJenaOWLModelFromURI(outputURI.toString());
                    owlModel.getRepositoryManager().addGlobalRepository(rep);

                    factory = new SWRLFactory(owlModel);
                    ruleEngine = SWRLRuleEngineFactory.create(owlModel);

                    factory.deleteImps();
                    if (i % 2 != 0) {
                        mainRule = factory.createImp("hasWebPR(?wj, ?prj) ∧ hasOutLink(?wj, ?wrel) ∧ wiki:hasOutLinkValue(?wrel, ?wi) ∧ hasTotalNrOutLinks(?wj, ?cj) ∧ totalNrWeb-Sites(Constant1, ?n) ∧ swrlm:eval(?yj, \"prj/cj\", ?prj, ?cj) ˚ sqwrl:makeBag(?s, ?yj) ∧ sqwrl:groupBy(?s, ?wi) ˚ sqwrl:sum(?ally, ?s)∧ swrlm:eval(?pri, \"0.15 / n + 0.85 * ally\", ?n, ?ally) → hasWebAlly(?wi, ?pri)");
                    } else {
                        mainRule = factory.createImp("hasWebAlly(?wj, ?prj) ∧ hasOutLink(?wj, ?wrel) ∧ wiki:hasOutLinkValue(?wrel, ?wi) ∧ hasTotalNrOutLinks(?wj, ?cj) ∧ totalNrWeb-Sites(Constant1, ?n) ∧ swrlm:eval(?yj, \"prj/cj\", ?prj, ?cj) ˚ sqwrl:makeBag(?s, ?yj) ∧ sqwrl:groupBy(?s, ?wi) ˚ sqwrl:sum(?ally, ?s)∧ swrlm:eval(?pri, \"0.15 / n + 0.85 * ally\", ?n, ?ally) → hasWebPR(?wi, ?pri)");
                    }

                    ruleEngine.infer();

                    errors = new ArrayList();
                    owlModel.save(new File("SemanticSearchOnto-v.1.0.owl").toURI(), FileUtils.langXMLAbbrev, errors);
                    System.out.println("File saved with " + errors.size() + " errors.");
                    i++;
                }
                Replace("hasWebAlly");

                status = jTextStatus.getText() + "\n" + " Rules Page Rank created";
                jTextStatus.setText(status);

                status = jTextStatus.getText() + "\n" + " Finished";
                jTextStatus.setText(status);

            } catch (SWRLParseException ex) {
                jTextStatus.setText(" Rule isn't defined in proper way");
                jTextStatus.setBackground(Color.ORANGE);
            }
        } catch (OntologyLoadException ex) {
            jTextStatus.setText(" Ontology couldn't found. Check Path.");
            jTextStatus.setBackground(Color.ORANGE);
        }

    }

    public void ExecuteAR() throws SQWRLException, SWRLRuleEngineException, InterruptedException, SWRLFactoryException, URISyntaxException {

        jTextStatus.setBackground(Color.white);

        int iterations = (Integer) spinnerIterations.getValue();

        if (iterations > 1) {
            iterations = iterations - 2;
        }
//        if (iterations % 2 == 0) {
//            iterations = iterations + 1;
//        }
        iterations = iterations * 2;
        try {

            try {


                URI outputURI = new URI(("file:/" + ONTOLOGY_URL.replaceAll(" ", "%20")));
                JenaOWLModel owlModel = ProtegeOWL.createJenaOWLModelFromURI(outputURI.toString());
                outputURI = new URI(("file:/" + Repository_Url.replaceAll(" ", "%20")));
                LocalFolderRepository rep = new LocalFolderRepository(new File(outputURI), true);
                owlModel.getRepositoryManager().addGlobalRepository(rep);
                outputURI = new URI(("file:/" + ONTOLOGY_URL.replaceAll(" ", "%20")));
                SWRLFactory factory = new SWRLFactory(owlModel);
                SWRLRuleEngine ruleEngine = SWRLRuleEngineFactory.create(owlModel);

                String status = "Ontology Loaded";

//                SQWRLQueryEngine queryEngine;
//                SQWRLResult result;
                //             queryEngine = SQWRLQueryEngineFactory.create(owlModel);

                factory.deleteImps();

                SWRLImp mainRule;
                mainRule = factory.createImp("Person(?ai) ∧ hasCo-author(?aj, ?rel) ∧ hasCo-authorValue(?rel, ?ai) ∧ hasCo-authorWeight(?rel, ?wji) ∧ sqwrl:makeBag(?s, ?wji) ∧ sqwrl:groupBy(?s, ?ai) ˚ sqwrl:sum(?ally, ?s) → hasAR(?ai, ?ally)");

                ruleEngine.infer();

                factory.deleteImps();

                mainRule = factory.createImp("Person(?ai) ∧ totalNrPersons(Constant1, ?n) ∧ hasAlly(?ai, ?ally) ∧ swrlm:eval(?ari, \"0.15 / n + 0.85 * ally\", ?n, ?ally) → hasAR(?ai, ?ari)");
                ruleEngine.infer();

                System.out.println("Saving...");
                Collection errors;

                errors = new ArrayList();

                owlModel.save(new File("co-authorOnto-v.2.0.owl").toURI(), FileUtils.langXMLAbbrev, errors);
                System.out.println("File saved with " + errors.size() + " errors.");
                int i = 0;
                while (i < iterations) {

                    if (i % 2 != 0) {
                        Replace("hasAR");
                    } else {
                        Replace("hasAlly");
                    }
                    System.out.println("Iteracion" + (i + 3));
                    //txtIteration.setText("Iteracion" + (i + 3));
                    owlModel.dispose();

                    mainRule = null;
                    owlModel = null;
                    factory = null;
                    ruleEngine = null;

                    System.gc();
                    java.lang.Runtime.getRuntime().freeMemory();
                    java.lang.Runtime.getRuntime().gc();


                    owlModel = ProtegeOWL.createJenaOWLModelFromURI(outputURI.toString());
                    owlModel.getRepositoryManager().addGlobalRepository(rep);

                    factory = new SWRLFactory(owlModel);
                    ruleEngine = SWRLRuleEngineFactory.create(owlModel);

                    factory.deleteImps();
                    if (i % 2 == 0) {
                        mainRule = factory.createImp("Person(?ai) ∧ hasCo-author(?aj, ?rel) ∧ hasCo-authorValue(?rel, ?ai) ∧ hasCo-authorWeight(?rel, ?wji) ∧ hasAR(?aj, ?arj) ∧ swrlm:eval(?yj, \"arj*wji\", ?arj, ?wji) ˚ sqwrl:makeBag(?s, ?yj) ∧ sqwrl:groupBy(?s, ?ai) ˚ sqwrl:sum(?ally, ?s) → hasAlly(?ai, ?ally) ");
                    } else {
                        mainRule = factory.createImp("Person(?ai) ∧ totalNrPersons(Constant1, ?n) ∧ hasAlly(?ai, ?ally) ∧ swrlm:eval(?ari, \"0.15 / n + 0.85 * ally\", ?n, ?ally) → hasAR(?ai, ?ari)");
                    }

                    ruleEngine.infer();

                    errors = new ArrayList();
                    owlModel.save(new File("co-authorOnto-v.2.0.owl").toURI(), FileUtils.langXMLAbbrev, errors);
                    System.out.println("File saved with " + errors.size() + " errors.");
                    i++;
                }
                Replace("hasAlly");

                status = jTextStatus.getText() + "\n" + " Rules Author Rank created";
                jTextStatus.setText(status);

                status = jTextStatus.getText() + "\n" + " Finished";
                jTextStatus.setText(status);

            } catch (SWRLParseException ex) {
                jTextStatus.setText(" Rule isn't defined in proper way");
                jTextStatus.setBackground(Color.ORANGE);
            }
        } catch (OntologyLoadException ex) {
            jTextStatus.setText(" Ontology couldn't found. Check Path.");
            jTextStatus.setBackground(Color.ORANGE);
        }

    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        mainPanel = new javax.swing.JPanel();
        jTextPanel = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTextRule = new javax.swing.JTextArea();
        jLabel5 = new javax.swing.JLabel();
        radioCalc = new javax.swing.JRadioButton();
        radioExec = new javax.swing.JRadioButton();
        radioPR = new javax.swing.JRadioButton();
        radioAR = new javax.swing.JRadioButton();
        spinnerIterations = new javax.swing.JSpinner();
        jLabel7 = new javax.swing.JLabel();
        jButtonExecute1 = new javax.swing.JButton();
        jPanel3 = new javax.swing.JPanel();
        jScrollPane3 = new javax.swing.JScrollPane();
        jTableResult = new javax.swing.JTable();
        jPanel1 = new javax.swing.JPanel();
        jLabel4 = new javax.swing.JLabel();
        jScrollPane2 = new javax.swing.JScrollPane();
        jTextStatus = new javax.swing.JTextArea();
        jLabel6 = new javax.swing.JLabel();
        txtStartTime = new javax.swing.JTextField();
        txtEndTime = new javax.swing.JTextField();
        jLabel9 = new javax.swing.JLabel();
        menuBar = new javax.swing.JMenuBar();
        javax.swing.JMenu fileMenu = new javax.swing.JMenu();
        javax.swing.JMenuItem exitMenuItem = new javax.swing.JMenuItem();
        javax.swing.JMenu helpMenu = new javax.swing.JMenu();
        javax.swing.JMenuItem aboutMenuItem = new javax.swing.JMenuItem();
        statusPanel = new javax.swing.JPanel();
        javax.swing.JSeparator statusPanelSeparator = new javax.swing.JSeparator();
        statusMessageLabel = new javax.swing.JLabel();
        statusAnimationLabel = new javax.swing.JLabel();
        progressBar = new javax.swing.JProgressBar();
        jFrame1 = new javax.swing.JFrame();
        mainAction = new javax.swing.ButtonGroup();
        radioCalcProp = new javax.swing.ButtonGroup();

        mainPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(""));
        mainPanel.setMaximumSize(new java.awt.Dimension(800, 600));
        mainPanel.setMinimumSize(new java.awt.Dimension(800, 600));
        mainPanel.setName("mainPanel"); // NOI18N
        mainPanel.setLayout(null);

        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(OwlPack.Owl.class).getContext().getResourceMap(OwlView.class);
        jTextPanel.setBackground(resourceMap.getColor("jTextPanel.background")); // NOI18N
        jTextPanel.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        jTextPanel.setName("jTextPanel"); // NOI18N
        jTextPanel.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jScrollPane1.setMaximumSize(new java.awt.Dimension(166, 32767));
        jScrollPane1.setName("jScrollPane1"); // NOI18N

        jTextRule.setColumns(20);
        jTextRule.setFont(resourceMap.getFont("jTextRule.font")); // NOI18N
        jTextRule.setRows(5);
        jTextRule.setMaximumSize(new java.awt.Dimension(164, 2147483647));
        jTextRule.setName("jTextRule"); // NOI18N
        jScrollPane1.setViewportView(jTextRule);

        jTextPanel.add(jScrollPane1, new org.netbeans.lib.awtextra.AbsoluteConstraints(130, 30, 640, 90));

        jLabel5.setText(resourceMap.getString("jLabel5.text")); // NOI18N
        jLabel5.setName("jLabel5"); // NOI18N
        jTextPanel.add(jLabel5, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 130, -1, -1));

        radioCalc.setBackground(resourceMap.getColor("radioPR.background")); // NOI18N
        mainAction.add(radioCalc);
        radioCalc.setText(resourceMap.getString("radioCalc.text")); // NOI18N
        radioCalc.setName("radioCalc"); // NOI18N
        radioCalc.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                radioCalcMouseClicked(evt);
            }
        });
        jTextPanel.add(radioCalc, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 10, -1, -1));

        radioExec.setBackground(resourceMap.getColor("radioPR.background")); // NOI18N
        mainAction.add(radioExec);
        radioExec.setText(resourceMap.getString("radioExec.text")); // NOI18N
        radioExec.setName("radioExec"); // NOI18N
        radioExec.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                radioExecMouseClicked(evt);
            }
        });
        jTextPanel.add(radioExec, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 70, -1, -1));

        radioPR.setBackground(resourceMap.getColor("radioPR.background")); // NOI18N
        radioCalcProp.add(radioPR);
        radioPR.setText(resourceMap.getString("radioPR.text")); // NOI18N
        radioPR.setName("radioPR"); // NOI18N
        jTextPanel.add(radioPR, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 30, -1, -1));

        radioAR.setBackground(resourceMap.getColor("radioPR.background")); // NOI18N
        radioCalcProp.add(radioAR);
        radioAR.setText(resourceMap.getString("hasAR.text")); // NOI18N
        radioAR.setName("hasAR"); // NOI18N
        jTextPanel.add(radioAR, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 50, -1, -1));

        spinnerIterations.setName("spinnerIterations"); // NOI18N
        spinnerIterations.setValue(1);
        jTextPanel.add(spinnerIterations, new org.netbeans.lib.awtextra.AbsoluteConstraints(130, 130, 120, -1));

        jLabel7.setText(resourceMap.getString("jLabel7.text")); // NOI18N
        jLabel7.setName("jLabel7"); // NOI18N
        jTextPanel.add(jLabel7, new org.netbeans.lib.awtextra.AbsoluteConstraints(130, 10, -1, -1));

        jButtonExecute1.setText(resourceMap.getString("jButtonExecute1.text")); // NOI18N
        jButtonExecute1.setName("jButtonExecute1"); // NOI18N
        jButtonExecute1.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jButtonExecute1MouseClicked(evt);
            }
        });
        jButtonExecute1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonExecute1ActionPerformed(evt);
            }
        });
        jTextPanel.add(jButtonExecute1, new org.netbeans.lib.awtextra.AbsoluteConstraints(500, 130, 270, 30));

        mainPanel.add(jTextPanel);
        jTextPanel.setBounds(0, 0, 780, 170);

        jPanel3.setBackground(resourceMap.getColor("jPanel3.background")); // NOI18N
        jPanel3.setName("jPanel3"); // NOI18N

        jScrollPane3.setName("jScrollPane3"); // NOI18N

        jTableResult.setFont(resourceMap.getFont("jTableResult.font")); // NOI18N
        jTableResult.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Result"
            }
        ));
        jTableResult.setName("jTableResult"); // NOI18N
        jScrollPane3.setViewportView(jTableResult);
        jTableResult.getColumnModel().getColumn(0).setHeaderValue(resourceMap.getString("jTableResult.columnModel.title0")); // NOI18N

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 1010, Short.MAX_VALUE))
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel3Layout.createSequentialGroup()
                .addContainerGap(13, Short.MAX_VALUE)
                .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 436, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        mainPanel.add(jPanel3);
        jPanel3.setBounds(0, 170, 1020, 460);

        jPanel1.setBackground(resourceMap.getColor("jPanel1.background")); // NOI18N
        jPanel1.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(-16777216,true)));
        jPanel1.setName("jPanel1"); // NOI18N

        jLabel4.setText(resourceMap.getString("jLabel4.text")); // NOI18N
        jLabel4.setName("jLabel4"); // NOI18N

        jScrollPane2.setName("jScrollPane2"); // NOI18N

        jTextStatus.setColumns(20);
        jTextStatus.setFont(resourceMap.getFont("jTextStatus.font")); // NOI18N
        jTextStatus.setRows(5);
        jTextStatus.setEnabled(false);
        jTextStatus.setName("jTextStatus"); // NOI18N
        jScrollPane2.setViewportView(jTextStatus);

        jLabel6.setText(resourceMap.getString("jLabel6.text")); // NOI18N
        jLabel6.setName("jLabel6"); // NOI18N

        txtStartTime.setEditable(false);
        txtStartTime.setText(resourceMap.getString("txtStartTime.text")); // NOI18N
        txtStartTime.setName("txtStartTime"); // NOI18N

        txtEndTime.setEditable(false);
        txtEndTime.setName("txtEndTime"); // NOI18N

        jLabel9.setText(resourceMap.getString("jLabel9.text")); // NOI18N
        jLabel9.setName("jLabel9"); // NOI18N

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 228, Short.MAX_VALUE)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jLabel6)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 24, Short.MAX_VALUE)
                        .addComponent(txtStartTime, javax.swing.GroupLayout.PREFERRED_SIZE, 155, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                        .addComponent(jLabel9)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 30, Short.MAX_VALUE)
                        .addComponent(txtEndTime, javax.swing.GroupLayout.PREFERRED_SIZE, 155, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jLabel4))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel6)
                    .addComponent(txtStartTime, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel9)
                    .addComponent(txtEndTime, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jLabel4)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 63, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(68, 68, 68))
        );

        mainPanel.add(jPanel1);
        jPanel1.setBounds(780, 0, 250, 170);

        menuBar.setName("menuBar"); // NOI18N

        fileMenu.setText(resourceMap.getString("fileMenu.text")); // NOI18N
        fileMenu.setName("fileMenu"); // NOI18N

        javax.swing.ActionMap actionMap = org.jdesktop.application.Application.getInstance(OwlPack.Owl.class).getContext().getActionMap(OwlView.class, this);
        exitMenuItem.setAction(actionMap.get("quit")); // NOI18N
        exitMenuItem.setName("exitMenuItem"); // NOI18N
        fileMenu.add(exitMenuItem);

        menuBar.add(fileMenu);

        helpMenu.setText(resourceMap.getString("helpMenu.text")); // NOI18N
        helpMenu.setName("helpMenu"); // NOI18N

        aboutMenuItem.setAction(actionMap.get("showAboutBox")); // NOI18N
        aboutMenuItem.setName("aboutMenuItem"); // NOI18N
        helpMenu.add(aboutMenuItem);

        menuBar.add(helpMenu);

        statusPanel.setName("statusPanel"); // NOI18N

        statusPanelSeparator.setName("statusPanelSeparator"); // NOI18N

        statusMessageLabel.setName("statusMessageLabel"); // NOI18N

        statusAnimationLabel.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        statusAnimationLabel.setName("statusAnimationLabel"); // NOI18N

        progressBar.setName("progressBar"); // NOI18N

        javax.swing.GroupLayout statusPanelLayout = new javax.swing.GroupLayout(statusPanel);
        statusPanel.setLayout(statusPanelLayout);
        statusPanelLayout.setHorizontalGroup(
            statusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(statusPanelSeparator, javax.swing.GroupLayout.DEFAULT_SIZE, 1030, Short.MAX_VALUE)
            .addGroup(statusPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(statusMessageLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 860, Short.MAX_VALUE)
                .addComponent(progressBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(statusAnimationLabel)
                .addContainerGap())
        );
        statusPanelLayout.setVerticalGroup(
            statusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(statusPanelLayout.createSequentialGroup()
                .addComponent(statusPanelSeparator, javax.swing.GroupLayout.PREFERRED_SIZE, 2, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(statusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(statusMessageLabel)
                    .addComponent(statusAnimationLabel)
                    .addComponent(progressBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(3, 3, 3))
        );

        jFrame1.setName("jFrame1"); // NOI18N

        javax.swing.GroupLayout jFrame1Layout = new javax.swing.GroupLayout(jFrame1.getContentPane());
        jFrame1.getContentPane().setLayout(jFrame1Layout);
        jFrame1Layout.setHorizontalGroup(
            jFrame1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 400, Short.MAX_VALUE)
        );
        jFrame1Layout.setVerticalGroup(
            jFrame1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 300, Short.MAX_VALUE)
        );

        setComponent(mainPanel);
        setMenuBar(menuBar);
        setStatusBar(statusPanel);
    }// </editor-fold>//GEN-END:initComponents

    private void jButtonExecute1MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jButtonExecute1MouseClicked

        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        Date dateStart = null, dateEnd;
        try {
            ClearModel();

            // txtIteration.setText("");
            dateStart = new Date();
            txtStartTime.setText(dateFormat.format(dateStart));
            if (radioPR.isSelected()) {
                ExecutePR();
                //String t = "g";
            } else if (radioAR.isSelected()) {
                ExecuteAR();
                //String t = "g";
            } else if (radioExec.isSelected()) {
                ExecuteRule();
            }

        } catch (URISyntaxException ex) {
            Logger.getLogger(OwlView.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SWRLFactoryException ex) {
            Logger.getLogger(OwlView.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SQWRLException ex) {
            Logger.getLogger(OwlView.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SWRLRuleEngineException ex) {
            Logger.getLogger(OwlView.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InterruptedException ex) {
            Logger.getLogger(OwlView.class.getName()).log(Level.SEVERE, null, ex);
        }
        dateEnd = new Date();
        txtEndTime.setText(dateFormat.format(dateEnd));
        dateFormat = new SimpleDateFormat("HH:mm:ss:SSS");
        long elapsed = dateEnd.getTime() - dateStart.getTime();
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT+0"));
        // txtElapsedTime.setText(dateFormat.format(new Date(elapsed)));

    }//GEN-LAST:event_jButtonExecute1MouseClicked
    private void radioCalcMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_radioCalcMouseClicked
        // TODO add your handling code here:
        radioAR.setEnabled(true);
        radioPR.setEnabled(true);
        radioCalcProp.clearSelection();
    }//GEN-LAST:event_radioCalcMouseClicked

    private void radioExecMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_radioExecMouseClicked
        // TODO add your handling code here:
        radioAR.setEnabled(false);
        radioPR.setEnabled(false);
        radioCalcProp.clearSelection();
    }//GEN-LAST:event_radioExecMouseClicked

private void jButtonExecute1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonExecute1ActionPerformed
// TODO add your handling code here:
}//GEN-LAST:event_jButtonExecute1ActionPerformed
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButtonExecute1;
    private javax.swing.JFrame jFrame1;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    public static javax.swing.JTable jTableResult;
    private javax.swing.JPanel jTextPanel;
    private javax.swing.JTextArea jTextRule;
    private javax.swing.JTextArea jTextStatus;
    private javax.swing.ButtonGroup mainAction;
    private javax.swing.JPanel mainPanel;
    private javax.swing.JMenuBar menuBar;
    private javax.swing.JProgressBar progressBar;
    private javax.swing.JRadioButton radioAR;
    private javax.swing.JRadioButton radioCalc;
    private javax.swing.ButtonGroup radioCalcProp;
    private javax.swing.JRadioButton radioExec;
    private javax.swing.JRadioButton radioPR;
    private javax.swing.JSpinner spinnerIterations;
    private javax.swing.JLabel statusAnimationLabel;
    private javax.swing.JLabel statusMessageLabel;
    private javax.swing.JPanel statusPanel;
    private javax.swing.JTextField txtEndTime;
    private javax.swing.JTextField txtStartTime;
    // End of variables declaration//GEN-END:variables
    private final Timer messageTimer;
    private final Timer busyIconTimer;
    private final Icon idleIcon;
    private final Icon[] busyIcons = new Icon[15];
    private int busyIconIndex = 0;
    private JDialog aboutBox;
    private DefaultTableModel model;
    private static String REGEX = ",";
    private Pattern pattern = Pattern.compile(REGEX);
    private final String ONTOLOGY_URL;
    private final String Repository_Url;
}
