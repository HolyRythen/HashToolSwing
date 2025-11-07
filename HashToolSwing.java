import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.dnd.*;
import java.awt.event.ActionEvent;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.List;
import java.util.*;
import java.util.concurrent.*;
import java.util.zip.CRC32;

public class HashToolSwing extends JFrame {
    // UI
    private final JTextField fileField = new JTextField();
    private final JButton browseBtn = new JButton("Datei wählen…");
    private final JButton calcBtn = new JButton("Hash berechnen");
    private final JButton clearBtn = new JButton("Leeren");
    private final JProgressBar progress = new JProgressBar();

    private final JTextField md5Field = new JTextField();
    private final JTextField sha1Field = new JTextField();
    private final JTextField sha256Field = new JTextField();
    private final JTextField sha512Field = new JTextField();
    private final JTextField crc32Field = new JTextField();

    private final JTextField compareField = new JTextField();
    private final JLabel compareResult = new JLabel("—");

    private final JTable batchTable = new JTable(new DefaultTableModel(new String[]{"Datei", "MD5", "SHA-256"}, 0));
    private final JButton batchAddBtn = new JButton("Batch: Dateien hinzufügen");
    private final JButton batchRunBtn = new JButton("Batch: Berechnen");
    private final JButton batchExportBtn = new JButton("Export CSV");

    // Exec
    private final ExecutorService pool = Executors.newFixedThreadPool(Math.max(2, Runtime.getRuntime().availableProcessors()));
    private volatile boolean busy = false;

    public HashToolSwing() {
        super("HashTool (Swing) – MD5 / SHA-1 / SHA-256 / SHA-512 / CRC32");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(900, 620));
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); SwingUtilities.updateComponentTreeUI(this); } catch (Exception ignored) {}

        // --- Top: Dateiauswahl ---
        JPanel top = new JPanel(new BorderLayout(8,8));
        fileField.setToolTipText("Datei hier ablegen oder Pfad einfügen");
        top.add(fileField, BorderLayout.CENTER);
        top.add(browseBtn, BorderLayout.EAST);

        // Drag&Drop robust: akzeptiert Dateiliste ODER Text (Pfad)
        new DropTarget(fileField, new DropTargetAdapter() {
            @Override public void drop(DropTargetDropEvent dtde) {
                try {
                    dtde.acceptDrop(DnDConstants.ACTION_COPY);
                    if (dtde.getTransferable().isDataFlavorSupported(java.awt.datatransfer.DataFlavor.javaFileListFlavor)) {
                        List<File> files = (List<File>) dtde.getTransferable()
                                .getTransferData(java.awt.datatransfer.DataFlavor.javaFileListFlavor);
                        if (!files.isEmpty()) fileField.setText(files.get(0).getAbsolutePath());
                    } else if (dtde.getTransferable().isDataFlavorSupported(java.awt.datatransfer.DataFlavor.stringFlavor)) {
                        String s = (String) dtde.getTransferable().getTransferData(java.awt.datatransfer.DataFlavor.stringFlavor);
                        if (s != null) {
                            // kann eine Liste mit CR/LF sein → erste Zeile nehmen
                            String first = s.split("\\r?\\n")[0];
                            fileField.setText(first.trim());
                        }
                    }
                } catch (Exception ignored) {}
            }
        });

        // --- Mitte: Hash Felder ---
        JPanel hashes = new JPanel(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(4,4,4,4);
        g.fill = GridBagConstraints.HORIZONTAL;
        g.weightx = 1;

        int r = 0;
        r = addHashRow(hashes, g, r, "MD5", md5Field);
        r = addHashRow(hashes, g, r, "SHA-1", sha1Field);
        r = addHashRow(hashes, g, r, "SHA-256", sha256Field);
        r = addHashRow(hashes, g, r, "SHA-512", sha512Field);
        r = addHashRow(hashes, g, r, "CRC32", crc32Field);

        // Vergleich
        JPanel cmp = new JPanel(new GridBagLayout());
        GridBagConstraints c2 = new GridBagConstraints();
        c2.insets = new Insets(4,4,4,4);
        c2.fill = GridBagConstraints.HORIZONTAL; c2.weightx=1;
        c2.gridx=0;c2.gridy=0; cmp.add(new JLabel("Vergleich gegen Hash (einfügen):"), c2);
        c2.gridx=0;c2.gridy=1; cmp.add(compareField, c2);
        c2.gridx=1;c2.gridy=1; cmp.add(new JButton(new AbstractAction("Prüfen") {
            @Override public void actionPerformed(ActionEvent e) { doCompare(); }
        }), c2);
        c2.gridx=0;c2.gridy=2; c2.gridwidth=2; cmp.add(compareResult, c2);
        compareResult.setFont(compareResult.getFont().deriveFont(Font.BOLD));
        compareResult.setForeground(new Color(60,60,60));

        // Buttons + Progress
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        actions.add(calcBtn);
        actions.add(clearBtn);
        progress.setStringPainted(true);
        progress.setPreferredSize(new Dimension(260, 22));
        actions.add(progress);

        // Batch
        JPanel batch = new JPanel(new BorderLayout(6,6));
        JPanel batchButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        batchButtons.add(batchAddBtn);
        batchButtons.add(batchRunBtn);
        batchButtons.add(batchExportBtn);
        batch.add(batchButtons, BorderLayout.NORTH);
        batch.add(new JScrollPane(batchTable), BorderLayout.CENTER);
        batchTable.setAutoCreateRowSorter(true);

        // Gesamt-Layout
        JPanel center = new JPanel(new BorderLayout(8,8));
        JPanel left = new JPanel(new BorderLayout(8,8));
        left.add(hashes, BorderLayout.CENTER);
        left.add(cmp, BorderLayout.SOUTH);

        center.add(left, BorderLayout.CENTER);
        center.add(batch, BorderLayout.SOUTH);

        add(top, BorderLayout.NORTH);
        add(center, BorderLayout.CENTER);
        add(actions, BorderLayout.SOUTH);

        // Aktionen
        browseBtn.addActionListener(e -> onBrowse());
        calcBtn.addActionListener(e -> onCalculate());
        clearBtn.addActionListener(e -> clearFields());
        batchAddBtn.addActionListener(e -> onBatchAdd());
        batchRunBtn.addActionListener(e -> onBatchRun());
        batchExportBtn.addActionListener(e -> onBatchExport());

        pack();
        setLocationRelativeTo(null);
    }

    private int addHashRow(JPanel p, GridBagConstraints g, int row, String label, JTextField field) {
        field.setEditable(false);
        JButton copyBtn = new JButton("Copy");
        copyBtn.addActionListener(e -> copyToClipboard(field.getText()));
        g.gridx=0; g.gridy=row; g.weightx=0; p.add(new JLabel(label), g);
        g.gridx=1; g.gridy=row; g.weightx=1; p.add(field, g);
        g.gridx=2; g.gridy=row; g.weightx=0; p.add(copyBtn, g);
        return row+1;
    }

    private void onBrowse() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Datei wählen");
        fc.setAcceptAllFileFilterUsed(true); // alle Dateien anzeigen
        // optionaler Filter:
        // fc.setFileFilter(new FileNameExtensionFilter("Alle Dateien (*.*)", "*"));
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            fileField.setText(fc.getSelectedFile().getAbsolutePath());
        }
    }

    private void onCalculate() {
        if (busy) return;
        String raw = fileField.getText().trim();
        if (raw.isEmpty()) { msg("Bitte eine Datei wählen oder hineinziehen."); return; }
        File f = resolveFile(raw);
        if (f == null) { msg("Pfad ungültig:\n" + raw); return; }
        if (!f.exists()) { msg("Datei existiert nicht:\n" + f.getAbsolutePath()); return; }
        if (!f.isFile()) { msg("Das ist kein Datei-Pfad (möglicherweise ein Ordner):\n" + f.getAbsolutePath()); return; }

        busy = true;
        progress.setIndeterminate(true);
        clearHashes();

        pool.submit(() -> {
            try {
                Map<String,String> res = calcAll(f);
                SwingUtilities.invokeLater(() -> {
                    md5Field.setText(res.get("MD5"));
                    sha1Field.setText(res.get("SHA-1"));
                    sha256Field.setText(res.get("SHA-256"));
                    sha512Field.setText(res.get("SHA-512"));
                    crc32Field.setText(res.get("CRC32"));
                    progress.setIndeterminate(false);
                    progress.setValue(100);
                    busy = false;
                });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    progress.setIndeterminate(false);
                    progress.setValue(0);
                    busy = false;
                    msg("Fehler: " + ex.getMessage());
                });
            }
        });
    }

    private void onBatchAdd() {
        JFileChooser fc = new JFileChooser();
        fc.setMultiSelectionEnabled(true);
        fc.setAcceptAllFileFilterUsed(true);
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            DefaultTableModel m = (DefaultTableModel) batchTable.getModel();
            for (File f : fc.getSelectedFiles()) {
                m.addRow(new Object[]{f.getAbsolutePath(), "", ""});
            }
        }
    }

    private void onBatchRun() {
        if (busy) return;
        DefaultTableModel m = (DefaultTableModel) batchTable.getModel();
        int n = m.getRowCount();
        if (n == 0) { msg("Keine Dateien in der Liste."); return; }
        busy = true;
        progress.setIndeterminate(false);
        progress.setMinimum(0);
        progress.setMaximum(n);
        progress.setValue(0);

        pool.submit(() -> {
            int done = 0;
            for (int i = 0; i < n; i++) {
                String path = String.valueOf(m.getValueAt(i, 0));
                File f = resolveFile(path);
                if (f == null || !f.isFile()) { done++; final int d=done; SwingUtilities.invokeLater(() -> progress.setValue(d)); continue; }
                try {
                    Map<String,String> res = calcAll(f);
                    final int row = i;
                    SwingUtilities.invokeLater(() -> {
                        m.setValueAt(res.get("MD5"), row, 1);
                        m.setValueAt(res.get("SHA-256"), row, 2);
                    });
                } catch (Exception ignored) {}
                done++;
                final int d = done;
                SwingUtilities.invokeLater(() -> progress.setValue(d));
            }
            SwingUtilities.invokeLater(() -> { busy = false; });
        });
    }

    private void onBatchExport() {
        DefaultTableModel m = (DefaultTableModel) batchTable.getModel();
        int n = m.getRowCount();
        if (n == 0) { msg("Keine Daten zum Export."); return; }

        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Als CSV speichern");
        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File out = fc.getSelectedFile();
            try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(out), "UTF-8"))) {
                pw.println("file,md5,sha256");
                for (int i = 0; i < n; i++) {
                    String file = csvEscape(String.valueOf(m.getValueAt(i,0)));
                    String md5 = csvEscape(String.valueOf(m.getValueAt(i,1)));
                    String sha = csvEscape(String.valueOf(m.getValueAt(i,2)));
                    pw.println(file + "," + md5 + "," + sha);
                }
                msg("Export gespeichert: " + out.getAbsolutePath());
            } catch (Exception ex) {
                msg("Export-Fehler: " + ex.getMessage());
            }
        }
    }

    private void clearFields() {
        fileField.setText("");
        clearHashes();
        ((DefaultTableModel) batchTable.getModel()).setRowCount(0);
        progress.setValue(0);
        progress.setIndeterminate(false);
        compareField.setText("");
        compareResult.setText("—");
        compareResult.setForeground(new Color(60,60,60));
    }
    private void clearHashes() {
        md5Field.setText(""); sha1Field.setText(""); sha256Field.setText(""); sha512Field.setText(""); crc32Field.setText("");
    }

    private void doCompare() {
        String target = compareField.getText().trim().toLowerCase(Locale.ROOT);
        if (target.isEmpty()) { compareResult.setText("—"); compareResult.setForeground(new Color(60,60,60)); return; }
        String md5 = md5Field.getText().trim().toLowerCase(Locale.ROOT);
        String s1 = sha1Field.getText().trim().toLowerCase(Locale.ROOT);
        String s256 = sha256Field.getText().trim().toLowerCase(Locale.ROOT);
        String s512 = sha512Field.getText().trim().toLowerCase(Locale.ROOT);
        String c32 = crc32Field.getText().trim().toLowerCase(Locale.ROOT);

        boolean match = target.equals(md5) || target.equals(s1) || target.equals(s256) || target.equals(s512) || target.equals(c32);
        compareResult.setText(match ? "✔ Hash stimmt überein" : "✖ Keine Übereinstimmung");
        compareResult.setForeground(match ? new Color(0,128,0) : new Color(180,0,0));
    }

    private void copyToClipboard(String s) {
        if (s == null || s.isEmpty()) return;
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(s), null);
    }

    private static String csvEscape(String s) {
        if (s == null) return "";
        if (s.contains(",") || s.contains("\"")) return "\"" + s.replace("\"","\"\"") + "\"";
        return s;
    }

    private static void msg(String s) {
        JOptionPane.showMessageDialog(null, s);
    }

    /** Pfad robust auflösen (entfernt Anführungszeichen, expandiert ~, normalisiert Backslashes). */
    private static File resolveFile(String raw) {
        if (raw == null) return null;
        String p = raw.trim();
        if (p.startsWith("\"") && p.endsWith("\"") && p.length() >= 2) {
            p = p.substring(1, p.length()-1);
        }
        // ~ → Benutzerordner
        if (p.startsWith("~" + File.separator) || p.equals("~")) {
            p = p.replaceFirst("^~", System.getProperty("user.home"));
        }
        // Windows-Backslashes normalisieren (falls aus Text kommt)
        p = p.replace('/', File.separatorChar);
        try {
            Path path = Paths.get(p).normalize();
            return path.toFile();
        } catch (Exception e) {
            return null;
        }
    }

    // --- Hash-Berechnung ---
    private static Map<String,String> calcAll(File f) throws Exception {
        Map<String,String> res = new LinkedHashMap<>();
        res.put("MD5", digestHex(f, "MD5"));
        res.put("SHA-1", digestHex(f, "SHA-1"));
        res.put("SHA-256", digestHex(f, "SHA-256"));
        res.put("SHA-512", digestHex(f, "SHA-512"));
        res.put("CRC32", crc32Hex(f));
        return res;
    }

    private static String digestHex(File f, String algo) throws Exception {
        MessageDigest md = MessageDigest.getInstance(algo);
        try (InputStream in = new BufferedInputStream(new FileInputStream(f));
             DigestInputStream dis = new DigestInputStream(in, md)) {
            byte[] buf = new byte[8192];
            while (dis.read(buf) != -1) { /* pumping */ }
        }
        byte[] d = md.digest();
        return toHex(d);
    }

    private static String crc32Hex(File f) throws IOException {
        CRC32 c = new CRC32();
        try (InputStream in = new BufferedInputStream(new FileInputStream(f))) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) != -1) c.update(buf, 0, n);
        }
        long v = c.getValue();
        return String.format("%08x", v);
    }

    private static String toHex(byte[] b) {
        StringBuilder sb = new StringBuilder(b.length * 2);
        for (byte value : b) sb.append(String.format("%02x", value));
        return sb.toString();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new HashToolSwing().setVisible(true));
    }
}
