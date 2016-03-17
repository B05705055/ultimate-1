/*
 * Copyright (C) 2013-2015 Daniel Dietsch (dietsch@informatik.uni-freiburg.de)
 * Copyright (C) 2008-2015 Jochen Hoenicke (hoenicke@informatik.uni-freiburg.de)
 * Copyright (C) 2015 University of Freiburg
 * 
 * This file is part of the ULTIMATE BoogiePLParser plug-in.
 * 
 * The ULTIMATE BoogiePLParser plug-in is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * The ULTIMATE BoogiePLParser plug-in is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ULTIMATE BoogiePLParser plug-in. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under GNU GPL version 3 section 7:
 * If you modify the ULTIMATE BoogiePLParser plug-in, or any covered work, by linking
 * or combining it with Eclipse RCP (or a modified version of Eclipse RCP), 
 * containing parts covered by the terms of the Eclipse Public License, the 
 * licensors of the ULTIMATE BoogiePLParser plug-in grant you additional permission 
 * to convey the resulting work.
 */
package de.uni_freiburg.informatik.ultimate.boogie.parser;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import de.uni_freiburg.informatik.ultimate.core.preferences.UltimatePreferenceInitializer;
import de.uni_freiburg.informatik.ultimate.core.services.model.IToolchainStorage;
import de.uni_freiburg.informatik.ultimate.core.services.model.IUltimateServiceProvider;
import de.uni_freiburg.informatik.ultimate.ep.interfaces.ISource;
import de.uni_freiburg.informatik.ultimate.model.GraphType;
import de.uni_freiburg.informatik.ultimate.model.IElement;
import de.uni_freiburg.informatik.ultimate.model.Payload;
import de.uni_freiburg.informatik.ultimate.model.boogie.ast.Declaration;
import de.uni_freiburg.informatik.ultimate.model.boogie.ast.Unit;
import de.uni_freiburg.informatik.ultimate.model.location.BoogieLocation;
import de.uni_freiburg.informatik.ultimate.model.location.ILocation;
import de.uni_freiburg.informatik.ultimate.model.structure.WrapperNode;

/**
 * This is the main Boogie 2 parser class that creates the lexer and parser to
 * parse Boogie 2 files into a CST.
 * 
 * @author hoenicke
 * 
 *         $LastChangedRevision: 544 $ $LastChangedDate: 2008-06-05 18:32:43
 *         +0200 (Do, 05 Jun 2008) $ $LastChangedBy: hoenicke $
 * 
 */
public class BoogieParser implements ISource {
	protected String[] mFileTypes;
	protected Logger mLogger;
	protected List<String> mFileNames;
	protected Unit mPreludeUnit;
	private IUltimateServiceProvider mServices;

	public BoogieParser() {
		mFileTypes = new String[] { "bpl" };
		mFileNames = new ArrayList<String>();
	}

	/**
	 * This method is required by IUltimatePlugin
	 * 
	 * @see de.uni_freiburg.informatik.ultimate.ep.interfaces.IToolchainPlugin#getPluginID()
	 */
	public String getPluginID() {
		return getClass().getPackage().getName();
	}

	/**
	 * This initializes the plugin. Parsers usually do not get parameters so we
	 * will just return 0 for anything.
	 * 
	 * @param param
	 *            is ignored
	 * 
	 * @see de.uni_freiburg.informatik.ultimate.ep.interfaces.IToolchainPlugin#init()
	 */
	public void init() {
		mFileNames = new ArrayList<String>();
	}

	/**
	 * This returns the name of the plugin
	 * 
	 * @return the name of the plugin
	 * @see de.uni_freiburg.informatik.ultimate.ep.interfaces.IParser#getPluginName()
	 */
	public String getPluginName() {
		return "Boogie PL CUP Parser";
	}

	/**
	 * This method uses reflection to return the TokenMap of the special parser
	 * 
	 * @return the tokens in a map
	 * @see de.uni_freiburg.informatik.ultimate.ep.interfaces.IParser#getTokens()
	 */
	public String[] getTokens() {
		return new String[] { "<invalid>", "<EOR>", "<DOWN>", "<UP>", "TYPE", "ARRAY_TYPE", "GENERIC_TYPE", "UNIT",
				"VARIABLE_DECLARATION", "TYPE_DECLARATION", "LITERAL", "METHOD", "AXIOM", "PROCEDURE", "BODY",
				"OPTIDTYPE", "OPTIDTYPELIST", "IDLIST", "CONSTANT", "CONSTANT_UNIQUE", "IDTYPE", "FUNCTION",
				"PARAMETERS", "RETURN_TYPE", "RETURNS", "VARIABLE", "SIGNATURE", "REQUIRES", "ENSURES", "FREE",
				"MODIFIES", "IMPLEMENTATION", "IDTYPELIST", "BLOCK", "GOTO", "RETURN", "ASSIGN", "LEFTHANDSIDE",
				"ASSIGNEXPRESSION", "ARRAYASSIGN", "CALL", "ASSERT", "ASSUME", "HAVOC", "INDEX", "EQUIVALENCE",
				"IMPLICATION", "ANDEXPR", "OREXPR", "RELATION", "TERM", "EXPRESSIONLIST", "DECIMAL_LITERAL",
				"BOOLEAN_LITERAL", "INT_LITERAL", "NOT", "NEG", "BV_LITERAL", "STRING_LITERAL", "CHAR_LITERAL",
				"HEX_LITERAL", "OCT_LITERAL", "NULL", "ARRAYEXPR", "ATOM", "ARITHMUL", "ARITHDIV", "ARITHMOD",
				"ARITHMINUS", "ARITHPLUS", "COMPEQ", "COMPNEQ", "COMPLT", "COMPLEQ", "COMPGT", "COMPGEQ",
				"COMPPARTORDER", "ID", "OLD", "CAST", "FORALL", "EXISTS", "TYPEID", "VARID", "Id", "BitvecLiteral",
				"IntegerLiteral", "BooleanLiteral", "WS", "COMMENT", "ATTTRIBUTES", "LINE_COMMENT", "IdCharacter",
				"Digit", "Number", "'bool'", "'int'", "'ref'", "'name'", "'any'", "'['", "','", "']'", "'<'", "'>'",
				"'type'", "';'", "'const'", "'unique'", "':'", "'function'", "'('", "')'", "'returns'", "'axiom'",
				"'var'", "'procedure'", "'free'", "'requires'", "'modifies'", "'ensures'", "'implementation'", "'{'",
				"'}'", "'goto'", "'return'", "':='", "'assert'", "'assume'", "'havoc'", "'call'", "'<==>'", "'==>'",
				"'&&'", "'||'", "'!'", "'-'", "'null'", "'old'", "'cast'", "'::'", "'=='", "'!='", "'>='", "'<='",
				"'<:'", "'+'", "'*'", "'/'", "'%'", "'forall'", "'exists'" };
	}

	/**
	 * Parses a list of files and constructs a tree with root node "PROJECT".
	 * This function uses reflection to get the parser, so make sure you set the
	 * correct one in your parser
	 * 
	 * @param files
	 *            an array of files to be parsed
	 * @return the tree
	 * @see de.uni_freiburg.informatik.ultimate.ep.interfaces.IParser#parseAST(java.io.File[])
	 */
	public IElement parseAST(File[] files) throws IOException {
		final WrapperNode dirRoot = new WrapperNode(null, null);

		for (File f : files) {
			Unit node = parseFile(f);
			dirRoot.addOutgoing(new WrapperNode(dirRoot, node));
		}
		return dirRoot;
	}

	/**
	 * Parses a file and constructs a tree. This function uses reflection to get
	 * the parser, so make sure you set the correct one in your parser
	 * 
	 * @param file
	 *            a file to be parsed
	 * @return the tree
	 * @see de.uni_freiburg.informatik.ultimate.ep.interfaces.IParser#parseAST(java.io.File)
	 */
	public IElement parseAST(File file) throws IOException {
		if (file.isDirectory()) {
			return parseAST(file.listFiles());
		} else {
			return parseFile(file);
		}
	}

	private Unit parseFile(File file) throws IOException {
		mLogger.info("Parsing: '" + file.getAbsolutePath() + "'");
		mFileNames.add(file.getAbsolutePath());
		return reflectiveParse(file.getAbsolutePath());
	}

	/**
	 * Gets a list of files and checks whether all of them are parseable by this
	 * parser.
	 * 
	 * @param files
	 *            a list of files to check
	 * @return true if parseable
	 * @see de.uni_freiburg.informatik.ultimate.ep.interfaces.IParser#parseable(java.io.File[])
	 */
	public boolean parseable(File[] files) {
		for (File f : files) {
			if (!parseable(f)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Gets a file and checks whether it is parseable by this parser.
	 * 
	 * @param file
	 *            the file to be checked
	 * @return true if parseable
	 * @see de.uni_freiburg.informatik.ultimate.ep.interfaces.IParser#parseable(java.io.File)
	 */
	public boolean parseable(File file) {
		for (String s : getFileTypes()) {
			if (file.getName().endsWith(s))
				return true;
		}
		return false;
	}

	/**
	 * get all supported file types of this parser
	 */
	public String[] getFileTypes() {
		return mFileTypes;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.uni_freiburg.informatik.ultimate.ep.interfaces.IOutputDefinition#
	 * getOutputDefinition()
	 */
	public GraphType getOutputDefinition() {
		try {
			return new GraphType(getPluginID(), GraphType.Type.AST, this.mFileNames);
		} catch (Exception ex) {
			mLogger.log(Level.FATAL, "syntax error: " + ex.getMessage());
			return null;
		}
	}

	/**
	 * This function parses the file given as argument. It is quite flexible so
	 * be careful using it
	 * 
	 * @param fileName
	 *            the file to be parsed
	 * @return an INode containing the AST
	 */
	private Unit reflectiveParse(String fileName) throws IOException {
		BoogieSymbolFactory symFactory = new BoogieSymbolFactory();
		Lexer lexer;
		Parser parser;
		Unit mainFile;

		lexer = new Lexer(new FileInputStream(fileName));
		lexer.setSymbolFactory(symFactory);
		parser = new Parser(lexer, symFactory, mServices);
		parser.setFileName(fileName);
		try {
			mainFile = (Unit) parser.parse().value;
		} catch (Exception e) {
			mLogger.fatal("syntax error: ", e);
			// TODO: Declare to throw a parser exception
			throw new RuntimeException(e);
		}
		if (mPreludeUnit != null) {
			Declaration[] prel = mPreludeUnit.getDeclarations();
			Declaration[] main = mainFile.getDeclarations();
			Declaration[] allDecls = new Declaration[prel.length + main.length];
			System.arraycopy(prel, 0, allDecls, 0, prel.length);
			System.arraycopy(main, 0, allDecls, prel.length, main.length);
			ILocation dummyLocation = new BoogieLocation(parser.mFilename, -1, -1, -1, -1, false);
			mainFile = new Unit(dummyLocation, allDecls);
		}
		return mainFile;
	}

	@Override
	public void setPreludeFile(File prelude) {
		mPreludeUnit = null;
		if (prelude == null)
			return;
		try {
			BoogieSymbolFactory symFactory = new BoogieSymbolFactory();
			Lexer lexer = new Lexer(new FileInputStream(prelude));
			lexer.setSymbolFactory(symFactory);
			Parser parser = new Parser(lexer, symFactory);
			parser.setFileName(prelude.getPath());
			mPreludeUnit = (Unit) parser.parse().value;
		} catch (Exception e) {
			mLogger.fatal("syntax error: ", e);
			throw new RuntimeException(e);
		}
	}

	@Override
	public UltimatePreferenceInitializer getPreferences() {
		return null;
	}

	@Override
	public void setToolchainStorage(IToolchainStorage services) {

	}

	@Override
	public void setServices(IUltimateServiceProvider services) {
		mServices = services;
		mLogger = mServices.getLoggingService().getLogger(Activator.s_PLUGIN_ID);
	}

	@Override
	public void finish() {
		// TODO Auto-generated method stub
		
	}
}
