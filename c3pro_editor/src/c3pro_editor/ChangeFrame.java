package c3pro_editor;

import java.awt.BorderLayout;
import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import net.miginfocom.swing.MigLayout;
import javax.swing.JTextField;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.ImageIcon;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.Color;


public class ChangeFrame extends JFrame {

	private JPanel contentPane;
	private JTextField txtFragmentstestbpmb;
	private JButton btnNewButton;
	private JButton btnNewButton_1;
	private JButton btnNewButton_2;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					ChangeFrame frame = new ChangeFrame();
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the frame.
	 */
	public ChangeFrame() {
		setTitle("Change Pattern: Insert");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 523, 325);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(new MigLayout("", "[grow]", "[25px:n,top][fill][25px:n,bottom]"));
		
		btnNewButton = new JButton("Load Fragment...");
		btnNewButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
			}
		});
		contentPane.add(btnNewButton, "flowx,cell 0 0,alignx center,aligny center");
		
		txtFragmentstestbpmb = new JTextField();
		txtFragmentstestbpmb.setText("c3pro/resources/models/fragment_1.bpmn");
		contentPane.add(txtFragmentstestbpmb, "cell 0 0,growx,aligny center");
		txtFragmentstestbpmb.setColumns(8);
		
		JLabel lblNewLabel = new JLabel("");
		lblNewLabel.setBackground(Color.WHITE);
		lblNewLabel.setIcon(new ImageIcon("/Users/conrad/Dropbox/Journal Change propagation/figures/fragment_in_new_dialog.png"));
		contentPane.add(lblNewLabel, "cell 0 1");
		
		btnNewButton_1 = new JButton("Ok");
		btnNewButton_1.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
			}
		});
		contentPane.add(btnNewButton_1, "flowx,cell 0 2,alignx right,aligny center");
		
		btnNewButton_2 = new JButton("Cancel");
		contentPane.add(btnNewButton_2, "cell 0 2,alignx right,aligny center");
	}

}
