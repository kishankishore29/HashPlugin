package hashplugin.handlers;

import java.awt.Component;
import java.awt.Container;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.jboss.forge.roaster.Roaster;
import org.jboss.forge.roaster.model.source.JavaClassSource;

public class Execution extends AbstractHandler {
	byte[] str;
	static boolean methodVisited = false;
	String hash_function = "";
	static long tim[] = new long[10];

	public Object execute(ExecutionEvent event) throws ExecutionException {

		IWorkbenchPart workbenchPart = PlatformUI.getWorkbench()
				.getActiveWorkbenchWindow().getActivePage().getActivePart();
		IFile file = (IFile) workbenchPart.getSite().getPage()
				.getActiveEditor().getEditorInput().getAdapter(IFile.class);

		final String filePath = file.getRawLocation().toOSString();
		
		// Get a list of the hash functions to be run on the program
		final JFrame frame = new JFrame("Hash Plugin");
		frame.setSize(300, 300);
		frame.setLocation(600, 300);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		JPanel entreePanel = new JPanel();
		final JPanel variablePanel = new JPanel();
		entreePanel.add(new JLabel("Choose the Hash Functions"));
		String[] hashFunctions = { "Fnv", "Jenkins", "Sdbm", "Djb2", "Ap",
				"Js", "Rs", "Bkdr", "Pjw", "Buz" };

		ButtonGroup group = new ButtonGroup();
		for (int i = 0; i < hashFunctions.length; i++) {
			JRadioButton button = new JRadioButton(hashFunctions[i]);
			group.add(button);
			variablePanel.add(button);

		}

		JPanel orderPanel = new JPanel();
		JButton button = new JButton("Next");
		orderPanel.add(button);

		Container content = frame.getContentPane();
		content.setLayout(new GridLayout(3, 1));
		variablePanel.setLayout(new GridLayout(5, 1));
		content.add(entreePanel);
		content.add(variablePanel);
		content.add(orderPanel);
		frame.setVisible(true);

		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				Component[] components = variablePanel.getComponents();

				for (int i = 0; i < components.length; i++) {
					JRadioButton cb = (JRadioButton) components[i];

					if (cb.isSelected())
						hash_function = cb.getText();

				}
				frame.dispose();
				try {
					HashMap<String, String> map = parse(filePath);
					getVariableNames(map, filePath);
				} catch (Exception e) {
					System.out.println("Error Occured");
				}

			}

		});

		return null;
	}

	public String readFileToString(String filePath) throws IOException {
		StringBuilder fileData = new StringBuilder(1000);
		BufferedReader reader = new BufferedReader(new FileReader(filePath));
		char[] buf = new char[10];
		int numRead = 0;
		while ((numRead = reader.read(buf)) != -1) {
			String readData = String.valueOf(buf, 0, numRead);
			fileData.append(readData);
			buf = new char[1024];
		}

		reader.close();

		return fileData.toString();
	}

	public void getVariableNames(final HashMap<String, String> map,
			final String filePath) {
		final JFrame frame = new JFrame("Hash Plugin");
		frame.setSize(400, 200);
		frame.setLocation(600, 300);

		frame.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent we) {
				System.exit(0);
			}
		});

		JPanel entreePanel = new JPanel();
		entreePanel.add(new JLabel(
				"Choose the variables to include while hashing"));
		final JPanel variablePanel = new JPanel();
		final Set<String> variableSet = new HashSet<String>();

		for (String key : map.keySet()) {
			variablePanel.add(new JCheckBox(key));
		}

		JPanel orderPanel = new JPanel();
		JButton button = new JButton("Start");
		orderPanel.add(button);

		Container content = frame.getContentPane();
		content.setLayout(new GridLayout(3, 1));
		content.add(entreePanel);
		content.add(variablePanel);
		content.add(orderPanel);

		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				Component[] components = variablePanel.getComponents();

				for (int i = 0; i < components.length; i++) {
					JCheckBox cb = (JCheckBox) components[i];

					if (cb.isSelected())
						variableSet.add(cb.getText());
					else
						map.remove(cb.getText());

				}
				try {
					autoGenerate(map, filePath);
				} catch (Exception e) {
					e.printStackTrace();
				}
				frame.dispose();
			}
		});

		frame.setVisible(true);

	}

	public void autoGenerate(HashMap<String, String> map, String filePath)
			throws Exception {

		generateEquals(map, filePath);
		generateHash();
		String code_equal = string_equals();
		String code_hashcode = string_hashcode();
		File file = new File(filePath);
		if (file.exists()) {
			JavaClassSource javaClass = Roaster.parse(JavaClassSource.class,
					file);

			javaClass.addInterface(Serializable.class);
			javaClass.addImport("hashplugin.handlers.Execution");
			javaClass.addMethod().setPublic().setStatic(false)
					.setName("equals").setReturnType("boolean")
					.setBody(code_equal).addParameter("Object", "obj");
			javaClass.addMethod().setPublic().setStatic(false)
					.setName("hashCode").setReturnType("int")
					.setBody(code_hashcode).addAnnotation(Override.class);

			FileWriter writer = new FileWriter(file);
			writer.write(javaClass.toString());
			writer.flush();
			writer.close();

		}
	}

	public void generateHash() throws IOException {
		FileWriter fw2 = new FileWriter("hash_code");
		BufferedWriter bw2 = new BufferedWriter(fw2);

		bw2.append("Execution object = new Execution();");
		switch (hash_function) {

		case "Ap":
			bw2.append("return object.apHash(this);");
			bw2.flush();
			break;

		case "Bkdr":
			bw2.write("return object.bkdrHash(this);");
			bw2.flush();
			break;

		case "Buz":
			bw2.write("return object.BuzHash(this);");
			bw2.flush();
			break;

		case "Djb2":
			bw2.write("return object.djb2(this);");
			bw2.flush();
			break;

		case "Fnv":
			bw2.write("return object.Fnv(this);");
			bw2.flush();
			break;

		case "Jenkins":
			bw2.write("return object.Jenkins(this);");
			bw2.flush();
			break;

		case "Js":
			bw2.write("return object.jsHash(this)");
			bw2.flush();
			break;

		case "Pjw":
			bw2.write("return object.pjwHash(this);");
			bw2.flush();
			break;

		case "Rs":
			bw2.write("return object.rsHash(this);");
			bw2.flush();
			break;

		case "Sdbm":
			bw2.write("return object.sdbmHash(this);");
			bw2.flush();
			break;
		}
	}

	public void generateEquals(HashMap<String, String> map, String filePath)
			throws IOException {
		FileWriter fw = new FileWriter("equals_code");
		String className = "";
		if (filePath.contains("/"))
			className = filePath.substring(filePath.lastIndexOf("/") + 1,
					filePath.length() - 5);
		if (filePath.contains("\\")){
			className = filePath.substring(filePath.lastIndexOf("\\") + 1,
					filePath.length() - 5);
				
		}
		
		BufferedWriter bw = new BufferedWriter(fw);
		bw.append("if (this == obj)\n");
		bw.append("\treturn true;\n");
		bw.append("if (obj == null)\n");
		bw.append("\treturn false;\n");
		bw.append("if(getClass() != obj.getClass())\n");
		bw.append("\treturn false;\n");
		bw.append(className + " other = (" + className + ") obj;\n");

		for (String itr : map.keySet()) {

			switch (map.get(itr)) {

			case "byte":

			case "short":

			case "int":

			case "long":
				bw.append("if(" + itr + "!=other." + itr + ")\n");
				bw.append("\treturn false;\n");
				bw.flush();
				break;

			case "double":
				bw.append("if(Double.doubleToLongBits(" + itr
						+ ")!= Double.doubleToLongBits(other." + itr + "))\n");
				bw.append("\treturn false;\n");
				bw.flush();
				break;

			case "float":
				bw.append("if (Float.floatToIntBits(" + itr
						+ ") != Float.floatToIntBits(other." + itr + "))\n");
				bw.append("\treturn false;\n");
				bw.flush();
				break;

			case "char":

				bw.append("if (" + itr + " != other." + itr + ")\n");
				bw.append("\treturn false;\n");
				bw.flush();
				break;

			case "String":

				bw.append("if (" + itr + " == null)\n");
				bw.append("{\n");
				bw.append("if ( other." + itr + " != null)\n");
				bw.append("\treturn false;\n");
				bw.append("}\n");
				bw.append("else if (!" + itr + ".equals(other." + itr + "))\n");
				bw.append("\treturn false;\n");
				bw.flush();
				break;
			}

		}
		bw.append("return true;");
		bw.close();

	}

	@SuppressWarnings("deprecation")
	public HashMap<String, String> parse(String filePath) throws IOException {
		String str = readFileToString(filePath);
		ASTParser parser = ASTParser.newParser(AST.JLS3);
		parser.setResolveBindings(true);
		parser.setSource(str.toCharArray());
		parser.setKind(ASTParser.K_COMPILATION_UNIT);

		final CompilationUnit cu = (CompilationUnit) parser.createAST(null);

		final HashMap<String, String> variableTypeMap = new HashMap<String, String>();

		cu.accept(new ASTVisitor() {
			public boolean visit(VariableDeclarationFragment node) {
				SimpleName name = node.getName();

				if (node.getParent() instanceof FieldDeclaration) {

					FieldDeclaration declaration = ((FieldDeclaration) node
							.getParent());
					if (!java.lang.reflect.Modifier.isStatic(declaration
							.getModifiers()))
						variableTypeMap.put(name.toString(), declaration
								.getType().toString());
				}
				return true;
			}

			public boolean visit(MethodDeclaration node) {
				if (!methodVisited)
					methodVisited = true;

				return false;
			}

		});
		return variableTypeMap;
	}

	public static String string_equals() throws IOException {
		BufferedReader br = new BufferedReader(new FileReader("equals_code"));
		try {
			StringBuilder equals = new StringBuilder();
			String line = br.readLine();

			while (line != null) {
				equals.append(line);
				equals.append("\n");
				line = br.readLine();
			}
			return equals.toString();
		} finally {
			br.close();
		}
	}

	public static String string_hashcode() throws IOException {
		BufferedReader br = new BufferedReader(new FileReader("hash_code"));
		try {
			StringBuilder equals = new StringBuilder();
			String line = br.readLine();

			while (line != null) {
				equals.append(line);
				equals.append("\n");
				line = br.readLine();
			}
			return equals.toString();
		} finally {
			br.close();
		}
	}

	public int apHash(Object user) {

		long init = System.nanoTime();
		str = serialize(user);
		int hash = 0;
		for (int i = 0; i < this.str.length; i++) {

			if ((i & 1) == 0) {
				hash ^= ((hash << 7) ^ str[i] ^ (hash >> 3));
			} else {
				hash ^= (~((hash << 11) ^ str[i] ^ (hash >> 5)));
			}
		}

		tim[0] += System.nanoTime() - init;
		return (int) (hash & 0x7FFFFFFF);
	}

	public int bkdrHash(Object user) {

		long init = System.nanoTime();
		str = serialize(user);
		long seed = 131; // 31 131 1313 13131 131313 etc..
		long hash = 0;

		for (int i = 0; i < str.length; i++) {

			hash = (hash * seed) + str[i];
		}
		tim[1] += System.nanoTime() - init;
		return (int) (hash & 0x7FFFFFFF);
	}

	public int BuzHash(Object user) {
		long init = System.nanoTime();
		str = serialize(user);
		long mix_master[/* 0:255 */] = {
		/* 000 */0x4476081a7043a46fL, 0x45768b8a6e7eac19L, 0xebd556c1cf055952L,
		/* */0x72ed2da1bf010101L, 0x3ff2030b128e8a64L,
		/* 005 */0xcbc330238adcfef2L, 0x737807fe42e20c6cL, 0x74dabaedb1095c58L,
		/* */0x968f065c65361d67L, 0xd3f4018ac7a4b199L,
		/* 010 */0x954b389b52f24df2L, 0x2f97a9d8d0549327L, 0xb9bea2b49a3b180fL,
		/* */0xaf2f42536b21f2ebL, 0x85d991663cff1325L,
		/* 015 */0xb9e1260207b575b9L, 0xf3ea88398a23b7e2L, 0xfaf8c83ffbd9091dL,
		/* */0x4274fe90834dbdf9L, 0x3f20b157b68d6313L,
		/* 020 */0x68b48972b6d06b93L, 0x694837b6eba548afL, 0xeecb51d1acc917c9L,
		/* */0xf1c633f02dffbcfaL, 0xa6549ec9d301f3b5L,
		/* 025 */0x451dc944f1663592L, 0x446d6acef6ce9e4fL, 0x1c8a5b3013206f02L,
		/* */0x5908ca36f2dc50f7L, 0x4fd55d3f3e880a87L,
		/* 030 */0xa03a8dbeabbf065dL, 0x3ccbbe078fabcb6dL, 0x1da53a259116f2d0L,
		/* */0xfb27a96fcb9af152L, 0x50aba242e85aec09L,
		/* 035 */0x24d4e414fc4fc987L, 0x83971844a9ce535eL, 0xc26a3fdeb849398eL,
		/* */0xc2380d044d2e70d8L, 0xab418aa8ae19b18fL,
		/* 040 */0xd95b6b9247d5ebeaL, 0x8b3b2171fdc60511L, 0xe15cd0ae3fcc44afL,
		/* */0x5a4e27f914a68f17L, 0x377bd28ca09aafdcL,
		/* 045 */0xbbeb9828594a3294L, 0x7c8df263ae1de1b9L, 0xba0a48a5fd1c1dd0L,
		/* */0x57cc1b8818b98ee6L, 0x8c570975d357dabcL,
		/* 050 */0x76bdcd6f2e8826aaL, 0x529b15b6ec4055f1L, 0x9147c7a54c34f8a9L,
		/* */0x2f96a7728170e402L, 0xe46602f455eca72eL,
		/* 055 */0x22834c4dd1bde03fL, 0x2644cf5a25e368ffL, 0x907c6de90b120f4aL,
		/* */0xadfe8ba99028f728L, 0xa85199ae14df0433L,
		/* 060 */0x2d749b946dd3601eL, 0x76e35457aa052772L, 0x90410bf6e427f736L,
		/* */0x536ad04d13e35041L, 0x8cc0d76769b76914L,
		/* 065 */0xae0249f6e3b3c01cL, 0x1bdfd075307d6fafL, 0xd8e04f70c221deccL,
		/* */0x4ab23622a4281a5dL, 0x37a5613da2fcaba7L,
		/* 070 */0x19a56203666d4a9fL, 0x158ffab502c4be93L, 0x0bee714e332ecb2fL,
		/* */0x69b71a59f6f74ab0L, 0x0fc7fc622f1dfe8fL,
		/* 075 */0x513966de7152a6f9L, 0xc16fae9cc2ea9be7L, 0xb66f0ac586c1899eL,
		/* */0x11e124aee3bdefd7L, 0x86cf5a577512901bL,
		/* 080 */0x33f33ba6994a1fbdL, 0xde6c4d1d3d47ff0dL, 0x6a99220dc6f78e66L,
		/* */0x2dc06ca93e2d25d2L, 0x96413b520134d573L,
		/* 085 */0xb4715ce8e1023afaL, 0xe6a75900c8c66c0aL, 0x6448f13ad54c12edL,
		/* */0xb9057c28cf6689f0L, 0xf4023daf67f7677aL,
		/* 090 */0x877c2650767b9867L, 0xb7ea587dcd5b2341L, 0xc048cf111733f9bcL,
		/* */0x112012c15bc867bfL, 0xc95f52b1d9418811L,
		/* 095 */0xa47e624ee7499083L, 0x26928606df9b12e8L, 0x5d020462ec3e0928L,
		/* */0x8bbde651f6d08914L, 0xd5db83db758e524aL,
		/* 100 */0x3105e355c000f455L, 0xdd7fe1b81a786c79L, 0x1f3a818c8e012db1L,
		/* */0xd902de819d7b42faL, 0x4200e63325cda5f0L,
		/* 105 */0x0e919cdc5fba9220L, 0x5360dd54605a11e1L, 0xa3182d0e6cb23e6cL,
		/* */0x13ee462c1b483b87L, 0x1b1b6087b997ee22L,
		/* 110 */0x81c36d0b877f7362L, 0xc24879932c1768d4L, 0x1faa756e1673f9adL,
		/* */0x61651b24d11fe93dL, 0x30fe3d9304e1cde4L,
		/* 115 */0x7be867c750747250L, 0x973e52c7005b5db6L, 0x75d6b699bbaf4817L,
		/* */0x25d2a9e97379e196L, 0xe65fb599aca98701L,
		/* 120 */0x6ac27960d24bde84L, 0xdfacc04c9fabbcb6L, 0xa46cd07f4a97882bL,
		/* */0x652031d8e59a1fd8L, 0x1185bd967ec7ce10L,
		/* 125 */0xfc9bd84c6780f244L, 0x0a0c59872f61b3ffL, 0x63885727a1c71c95L,
		/* */0x5e88b4390b2d765cL, 0xf0005ccaf988514dL,
		/* 130 */0x474e44280a98e840L, 0x32de151c1411bc42L, 0x2c4b86d5aa4482c2L,
		/* */0xccd93deb2d9d47daL, 0x3743236ff128a622L,
		/* 135 */0x42ed2f2635ba5647L, 0x99c74afd18962dbdL, 0x2d663bb870f6d242L,
		/* */0x7912033bc7635d81L, 0xb442862f43753680L,
		/* 140 */0x94b1a5400aeaab4cL, 0x5ce285fe810f2220L, 0xe8a7dbe565d9c0b1L,
		/* */0x219131af78356c94L, 0x7b3a80d130f27e2fL,
		/* 145 */0xbaa5d2859d16b440L, 0x821cfb6935771070L, 0xf68cfb6ee9bc2336L,
		/* */0x18244132e935d2fdL, 0x2ed0bda1f4720cffL,
		/* 150 */0x4ed48cdf6975173cL, 0xfd37a7a2520e2405L, 0x82c102b2a9e73ce2L,
		/* */0xadac6517062623a7L, 0x5a1294d318e26104L,
		/* 155 */0xea84fe65c0e4f061L, 0x4f96f8a9464cfee9L, 0x9831dff8ccdc534aL,
		/* */0x4ca927cd0f192a14L, 0x030900b294b71649L,
		/* 160 */0x644b263b9aeb0675L, 0xa601d4e34647e040L, 0x34d897eb397f1004L,
		/* */0xa6101c37f4ec8dfcL, 0xc29d2a8bbfd0006bL,
		/* 165 */0xc6b07df8c5b4ed0fL, 0xce1b7d92ba6bccbeL, 0xfa2f99442e03fe1bL,
		/* */0xd8863e4c16f0b363L, 0x033b2cccc3392942L,
		/* 170 */0x757dc33522d6cf9cL, 0xf07b1ff6ce55fec5L, 0x1569e75f09b40463L,
		/* */0xfa33fa08f14a310bL, 0x6eb79aa27bbcf76bL,
		/* 175 */0x157061207c249602L, 0x25e5a71fc4e99555L, 0x5df1fe93de625355L,
		/* */0x235b56090c1aa55dL, 0xe51068613eaced91L,
		/* 180 */0x45bd47b893b9ff1eL, 0x6595e1798d381f2dL, 0xc9b5848cbcdb5ba8L,
		/* */0x65985146ff7792bcL, 0x4ab4a17bf05a19a0L,
		/* 185 */0xfd94f4ca560ffb0cL, 0xcf9bad581a68fa68L, 0x92b4f0b502b1ce1aL,
		/* */0xbcbec0769a610474L, 0x8dbd31ded1a0fecbL,
		/* 190 */0xdd1f5ed9f90e8533L, 0x61c1e6a523f84d95L, 0xf24475f383c110c4L,
		/* */0xdb2dffa66f90588dL, 0xac06d88e9ee04455L,
		/* 195 */0xa215fc47c40504baL, 0x86d7caebfee93369L, 0x9eaec31985804099L,
		/* */0x0fba2214abe5d01bL, 0x5a32975a4b3865d6L,
		/* 200 */0x8cceebc98a5c108fL, 0x7e12c4589654f2dcL, 0xa49ad49fb0d19772L,
		/* */0x3d142dd9c406152bL, 0x9f13589e7be2b8a5L,
		/* 205 */0x5e8dbac1892967adL, 0xcc23b93a6308e597L, 0x1ef35f5fe874e16aL,
		/* */0x63ae9cc08d2e274fL, 0x5bbabee56007fc05L,
		/* 210 */0xabfd72994230fc39L, 0x9d71a13a99144de1L, 0xd9daf5aa8dcc89b3L,
		/* */0xe145ec0514161bfdL, 0x143befc2498cd270L,
		/* 215 */0xa8e192557dbbd9f8L, 0xcbeda2445628d7d0L, 0x997f0a93205d9ea4L,
		/* */0x01014a97f214ebfaL, 0x70c026ffd1ebedafL,
		/* 220 */0xf8737b1b3237002fL, 0x8afcbef3147e6e5eL, 0x0e1bb0684483ebd3L,
		/* */0x4cbad70ae9b05aa6L, 0xd4a31f523517c363L,
		/* 225 */0xdb0f057ae8e9e8a2L, 0x400894a919d89df6L, 0x6a626a9b62defab3L,
		/* */0xf907fd7e14f4e201L, 0xe10e4a5657c48f3fL,
		/* 230 */0xb17f9f54b8e6e5dcL, 0x6b9e69045fa6d27aL, 0x8b74b6a41dc3078eL,
		/* */0x027954d45ca367f9L, 0xd07207b8fdcbb7ccL,
		/* 235 */0xf397c47d2f36414bL, 0x05e4e8b11d3a034fL, 0x36adb3f7122d654fL,
		/* */0x607d9540eb336078L, 0xb639118e3a8b9600L,
		/* 240 */0xd0a406770b5f1484L, 0x3cbee8213ccfb7c6L, 0x467967bb2ff89cf1L,
		/* */0xb115fe29609919a6L, 0xba740e6ffa83287eL,
		/* 245 */0xb4e51be9b694b7cdL, 0xc9a081c677df5aeaL, 0x2e1fbcd8944508ccL,
		/* */0xf626e7895581fbb8L, 0x3ce6e9b5728a05cbL,
		/* 250 */0x46e87f2664a31712L, 0x8c1dc526c2f6acfaL, 0x7b4826726e560b10L,
		/* */0x2966e0099d8d7ce1L, 0xbb0dd5240d2b2adeL, 0x0d527cc60bbaa936L };

		long h = 0xe12398c6d9ae3b8aL;
		for (int i = 0; i < str.length; ++i)
			h = (h << 1) ^ (h >>> 63)
					^ mix_master[(str[i] ^ (str[i] >>> 8)) & 0xff];
		tim[2] += System.nanoTime() - init;
		return (int) (h & 0x7FFFFFFF);
	}

	public int djb2(Object user) {

		long init = System.nanoTime();
		str = serialize(user);
		int hash = 5381;

		for (int i = 0; i < str.length; i++) {

			hash = ((hash << 5) + hash) + str[i];
		}
		tim[3] += System.nanoTime() - init;
		return hash & 0x7FFFFFFF;
	}

	public int Fnv(Object user) {

		long init = System.nanoTime();
		str = serialize(user);
		long hash = 0x811c9dc5L;

		for (byte b : str) {
			hash = (hash * 0x01000193L) % (0x100000000L);
			hash = hash ^ (Long.valueOf((int) b & 0xff));
		}
		tim[4] += System.nanoTime() - init;
		return (int) (hash & 0x7FFFFFFF);
	}

	public int Jenkins(Object user) {
		long init = System.nanoTime();
		str = serialize(user);
		int a, b, c;
		int pc = 0;
		int pb = 0;
		boolean is32BitHash = true;
		int length = str.length;

		a = b = c = 0xdeadbeef + length + pc;
		c += pb;

		int offset = 0;
		while (length > 12) {
			a += str[offset + 0];
			a += str[offset + 1] << 8;
			a += str[offset + 2] << 16;
			a += str[offset + 3] << 24;
			b += str[offset + 4];
			b += str[offset + 5] << 8;
			b += str[offset + 6] << 16;
			b += str[offset + 7] << 24;
			c += str[offset + 8];
			c += str[offset + 9] << 8;
			c += str[offset + 10] << 16;
			c += str[offset + 11] << 24;

			// mix(a, b, c);
			a -= c;
			a ^= rot(c, 4);
			c += b;
			b -= a;
			b ^= rot(a, 6);
			a += c;
			c -= b;
			c ^= rot(b, 8);
			b += a;
			a -= c;
			a ^= rot(c, 16);
			c += b;
			b -= a;
			b ^= rot(a, 19);
			a += c;
			c -= b;
			c ^= rot(b, 4);
			b += a;

			length -= 12;
			offset += 12;
		}

		switch (length) {
		case 12:
			c += str[offset + 11] << 24;
		case 11:
			c += str[offset + 10] << 16;
		case 10:
			c += str[offset + 9] << 8;
		case 9:
			c += str[offset + 8];
		case 8:
			b += str[offset + 7] << 24;
		case 7:
			b += str[offset + 6] << 16;
		case 6:
			b += str[offset + 5] << 8;
		case 5:
			b += str[offset + 4];
		case 4:
			a += str[offset + 3] << 24;
		case 3:
			a += str[offset + 2] << 16;
		case 2:
			a += str[offset + 1] << 8;
		case 1:
			a += str[offset + 0];
			break;
		case 0:
			return (int) (is32BitHash ? c : ((((long) c) << 32))
					| ((long) b & 0xFFFFFFFFL));
		}

		// Final mixing of three 32-bit values in to c
		c ^= b;
		c -= rot(b, 14);
		a ^= c;
		a -= rot(c, 11);
		b ^= a;
		b -= rot(a, 25);
		c ^= b;
		c -= rot(b, 16);
		a ^= c;
		a -= rot(c, 4);
		b ^= a;
		b -= rot(a, 14);
		c ^= b;
		c -= rot(b, 24);

		tim[5] += System.nanoTime() - init;
		return (int) (is32BitHash ? c : ((((long) c) << 32))
				| ((long) b & 0xFFFFFFFFL)) & 0x7FFFFFFF;
	}

	public int jsHash(Object user) {
		long init = System.nanoTime();
		str = serialize(user);
		long hash = 1315423911;

		for (int i = 0; i < str.length; i++) {
			hash ^= ((hash << 5) + str[i] + (hash >> 2));
		}
		tim[6] += System.nanoTime() - init;
		return (int) (hash & 0x7FFFFFFF);
	}

	public int pjwHash(Object user) {
		long init = System.nanoTime();
		str = serialize(user);
		long BitsInUnignedInt = (long) (4 * 8);
		long ThreeQuarters = (long) ((BitsInUnignedInt * 3) / 4);
		long OneEighth = (long) (BitsInUnignedInt / 8);
		long HighBits = (long) (0xFFFFFFFF) << (BitsInUnignedInt - OneEighth);
		long hash = 0;
		long test = 0;

		for (int i = 0; i < str.length; i++) {

			hash = (hash << OneEighth) + str[i];

			if ((test = hash & HighBits) != 0) {

				hash = ((hash ^ (test >> ThreeQuarters)) & (~HighBits));

			}

		}
		tim[7] += System.nanoTime() - init;
		return (int) (hash & 0x7FFFFFFF);
	}

	public int rsHash(Object user) {
		long init = System.nanoTime();
		str = serialize(user);
		int b = 378551;
		int a = 63689;
		long hash = 0;

		for (int i = 0; i < str.length; i++) {
			hash = hash * a + str[i];
			a = a * b;
		}
		tim[8] += System.nanoTime() - init;
		return (int) (hash & 0x7FFFFFFF);

	}

	public int sdbmHash(Object user) {
		long init = System.nanoTime();
		str = serialize(user);
		int hash = 0;

		for (int i = 0; i < str.length; i++) {

			hash = str[i] + (hash << 6) + (hash << 16) - hash;

		}
		tim[9] += System.nanoTime() - init;
		return (int) (hash & 0x7FFFFFFF);
	}

	static long rot(int x, int distance) {
		return (x << distance) | (x >>> (32 - distance));
		// return (x << distance) | (x >>> -distance);
	}

	public static byte[] serialize(Object obj) {
		try {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			ObjectOutputStream os = new ObjectOutputStream(out);
			os.writeObject(obj);
			return out.toByteArray();
		} catch (Exception e) {

		}
		return null;
	}

	public static void getStatistics() throws IOException {

		FileWriter fw = new FileWriter(
				"output", true);
		BufferedWriter bw = new BufferedWriter(fw);

		String result = "";
		String hashFunctions[] = { "ApHash", "Bkdr", "BuzHash", "Djb2", "Fnv",
				"Jenkins", "js", "pjw", "rs", "sdbm" };
		for (int i = 0; i < tim.length; i++) {
			if (tim[i] != 0) {
				result += "Time for " + hashFunctions[i] + " :"
						+ (tim[i] / Math.pow(10, 9)) + " nanoseconds\n";

				bw.append(hashFunctions[i] + ":" + tim[i] + " nanoseconds \n");
			}
		}

		JOptionPane.showMessageDialog(null, result);
		bw.close();
	}

	public static void summary() throws IOException {
		FileReader fReader = new FileReader(
				"output");
		BufferedReader br = new BufferedReader(fReader);
		StringBuilder sb = new StringBuilder();
		String currentLine = "";
		while ((currentLine = br.readLine()) != null) {

			sb.append(currentLine);
			sb.append("\n");
		}
		br.close();
		JOptionPane.showMessageDialog(null, sb);
	}

	public static void close() throws IOException {

		FileWriter file = new FileWriter(
				"output", false);
		PrintWriter writer = new PrintWriter(file);
		writer.print("");
		writer.close();
	}
}