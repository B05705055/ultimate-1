/*
 * Copyright (C) 2013-2015 Daniel Dietsch (dietsch@informatik.uni-freiburg.de)
 * Copyright (C) 2012-2015 Markus Lindenmann (lindenmm@informatik.uni-freiburg.de)
 * Copyright (C) 2012-2015 Matthias Heizmann (heizmann@informatik.uni-freiburg.de)
 * Copyright (C) 2015 Oleksii Saukh (saukho@informatik.uni-freiburg.de)
 * Copyright (C) 2015 Stefan Wissert
 * Copyright (C) 2015 University of Freiburg
 * 
 * This file is part of the ULTIMATE CACSL2BoogieTranslator plug-in.
 * 
 * The ULTIMATE CACSL2BoogieTranslator plug-in is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * The ULTIMATE CACSL2BoogieTranslator plug-in is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ULTIMATE CACSL2BoogieTranslator plug-in. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under GNU GPL version 3 section 7:
 * If you modify the ULTIMATE CACSL2BoogieTranslator plug-in, or any covered work, by linking
 * or combining it with Eclipse RCP (or a modified version of Eclipse RCP), 
 * containing parts covered by the terms of the Eclipse Public License, the 
 * licensors of the ULTIMATE CACSL2BoogieTranslator plug-in grant you additional permission 
 * to convey the resulting work.
 */
/**
 * Describes a dispatcher.
 */
package de.uni_freiburg.informatik.ultimate.cdt.translation.interfaces;

import java.text.ParseException;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;

import org.apache.log4j.Logger;
import org.eclipse.cdt.core.dom.ast.IASTDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTPreprocessorStatement;
import org.eclipse.cdt.core.dom.ast.IType;

import de.uni_freiburg.informatik.ultimate.cdt.decorator.DecoratorNode;
import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.base.NextACSL;
import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.base.cHandler.TypeSizes;
import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.container.InferredType;
import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.result.Result;
import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.util.SFO;
import de.uni_freiburg.informatik.ultimate.cdt.translation.interfaces.handler.IACSLHandler;
import de.uni_freiburg.informatik.ultimate.cdt.translation.interfaces.handler.ICHandler;
import de.uni_freiburg.informatik.ultimate.cdt.translation.interfaces.handler.INameHandler;
import de.uni_freiburg.informatik.ultimate.cdt.translation.interfaces.handler.IPreprocessorHandler;
import de.uni_freiburg.informatik.ultimate.cdt.translation.interfaces.handler.ISideEffectHandler;
import de.uni_freiburg.informatik.ultimate.cdt.translation.interfaces.handler.ITypeHandler;
import de.uni_freiburg.informatik.ultimate.core.preferences.UltimatePreferenceStore;
import de.uni_freiburg.informatik.ultimate.core.services.IUltimateServiceProvider;
import de.uni_freiburg.informatik.ultimate.model.IElement;
import de.uni_freiburg.informatik.ultimate.model.acsl.ACSLNode;
import de.uni_freiburg.informatik.ultimate.model.location.ILocation;
import de.uni_freiburg.informatik.ultimate.plugins.generator.cacsl2boogietranslator.Activator;
import de.uni_freiburg.informatik.ultimate.plugins.generator.cacsl2boogietranslator.CACSL2BoogieBacktranslator;
import de.uni_freiburg.informatik.ultimate.plugins.generator.cacsl2boogietranslator.preferences.CACSLPreferenceInitializer;
import de.uni_freiburg.informatik.ultimate.plugins.generator.cacsl2boogietranslator.preferences.CACSLPreferenceInitializer.POINTER_CHECKMODE;
import de.uni_freiburg.informatik.ultimate.result.GenericResultAtLocation;
import de.uni_freiburg.informatik.ultimate.result.IResultWithSeverity.Severity;
import de.uni_freiburg.informatik.ultimate.result.SyntaxErrorResult;
import de.uni_freiburg.informatik.ultimate.result.UnsupportedSyntaxResult;

/**
 * @author Markus Lindenmann
 * @author Oleksii Saukh
 * @author Stefan Wissert
 * @date 01.02.2012
 */
public abstract class Dispatcher {

	protected LinkedHashMap<String, Integer> mFunctionToIndex;

	protected final Logger mLogger;

	public UltimatePreferenceStore mPreferences;

	/**
	 * The side effect handler.
	 */
	public ISideEffectHandler sideEffectHandler;
	/**
	 * The C+ACSL handler.
	 */
	public ICHandler cHandler;
	/**
	 * The Type handler.
	 */
	public ITypeHandler typeHandler;
	/**
	 * The ACSL handler.
	 */
	public IACSLHandler acslHandler;
	/**
	 * The Name handler.
	 */
	public INameHandler nameHandler;
	/**
	 * Holds the next ACSL node in the decorator tree.
	 */
	public NextACSL nextAcsl;
	/**
	 * The Preprocessor statement handler.
	 */
	public IPreprocessorHandler preprocessorHandler;
	/**
	 * This plugin creates results for warnings if set to true.
	 */
	protected static boolean REPORT_WARNINGS = true;
	/**
	 * Translation from Boogie to C for traces and expressions.
	 */
	protected final CACSL2BoogieBacktranslator backtranslator;
	protected final IUltimateServiceProvider mServices;
	

	private final TypeSizes m_TypeSizes;

	private final TranslationSettings m_TranslationSettings;

	public Dispatcher(CACSL2BoogieBacktranslator backtranslator, IUltimateServiceProvider services, Logger logger) {
		this.backtranslator = backtranslator;
		mLogger = logger;
		mServices = services;
		mPreferences = new UltimatePreferenceStore(Activator.s_PLUGIN_ID);
		m_TypeSizes = new TypeSizes(mPreferences);
		m_TranslationSettings = new TranslationSettings(mPreferences);
	}

	/**
	 * Initializes the handler fields.
	 */
	protected abstract void init();

	/**
	 * Dispatch a given node to a specific handler.
	 * 
	 * @param node
	 *            the node to dispatch
	 * @return the result for the given node
	 */
	public abstract Result dispatch(DecoratorNode node);

	/**
	 * Dispatch a given C node to a specific handler.
	 * 
	 * @param node
	 *            the node to dispatch
	 * @return the result for the given node
	 */
	public abstract Result dispatch(IASTNode node);

	/**
	 * Dispatch a given C node to a specific handler.
	 * 
	 * @param node
	 *            the node to dispatch.
	 * @return the resulting translation.
	 */
	public abstract Result dispatch(IASTPreprocessorStatement node);

	/**
	 * Dispatch a given IType to a specific handler.
	 * 
	 * @param type
	 *            the type to dispatch
	 * @return the result for the given type.
	 */
	public abstract InferredType dispatch(IType type);

	/**
	 * Dispatch a given ACSL node to the specific handler.
	 * 
	 * @param node
	 *            the node to dispatch
	 * @return the result for the given node
	 */
	public abstract Result dispatch(ACSLNode node);

	/**
	 * Entry point for a translation.
	 * 
	 * @param node
	 *            the root node from which the translation should be started
	 * @return the result for the given node
	 */
	public final Result run(DecoratorNode node) {
		preRun(node);
		init();
		return dispatch(node);
	}

	/**
	 * The method implementing a pre-run, if required.
	 * 
	 * @param node
	 *            the node for which the pre run should be started
	 */
	protected abstract void preRun(DecoratorNode node);

	/**
	 * Iterates to the next ACSL statement in the decorator tree and returns a
	 * list of ACSL nodes until the next C node appears.
	 * 
	 * @return a list of ACSL nodes until the next C node appears.
	 * @throws ParseException
	 *             if no trailing C node in the tree! The ACSL is in an
	 *             unexpected and most probably unreachable location and should
	 *             be ignored!
	 */
	public abstract NextACSL nextACSLStatement() throws ParseException;

	// /**
	// * Report a syntax error to Ultimate. This will cancel the toolchain.
	// *
	// * @param loc
	// * where did it happen?
	// * @param type
	// * why did it happen?
	// * @param msg
	// * description.
	// */
	// public static void error(ILocation loc, SyntaxErrorType type, String msg)
	// {
	// SyntaxErrorResult<ILocation> result = new SyntaxErrorResult<ILocation>(
	// loc, Activator.s_PLUGIN_NAME, UltimateServices.getInstance()
	// .getTranslatorSequence(), loc, type);
	// result.setLongDescription(msg);
	// UltimateServices us = UltimateServices.getInstance();
	// us.getLogger(Activator.s_PLUGIN_ID).warn(msg);
	// us.reportResult(Activator.s_PLUGIN_ID, result);
	// us.cancelToolchain();
	// }

	/**
	 * Report a syntax error to Ultimate. This will cancel the toolchain.
	 * 
	 * @param loc
	 *            where did it happen?
	 * @param type
	 *            why did it happen?
	 * @param msg
	 *            description.
	 */
	public void syntaxError(ILocation loc, String msg) {
		SyntaxErrorResult result = new SyntaxErrorResult(Activator.s_PLUGIN_NAME, loc, msg);
		mLogger.warn(msg);
		mServices.getResultService().reportResult(Activator.s_PLUGIN_ID, result);
		mServices.getProgressMonitorService().cancelToolchain();
	}

	/**
	 * Report a unsupported syntax to Ultimate. This will cancel the toolchain.
	 * 
	 * @param loc
	 *            where did it happen?
	 * @param type
	 *            why did it happen?
	 * @param msg
	 *            description.
	 */
	public void unsupportedSyntax(ILocation loc, String msg) {
		UnsupportedSyntaxResult<IElement> result = new UnsupportedSyntaxResult<IElement>(Activator.s_PLUGIN_NAME, loc,
				msg);
		mLogger.warn(msg);
		mServices.getResultService().reportResult(Activator.s_PLUGIN_ID, result);
		mServices.getProgressMonitorService().cancelToolchain();
	}

	/**
	 * Report possible source of unsoundness to Ultimate.
	 * 
	 * @param loc
	 *            where did it happen?
	 * @param longDesc
	 *            description.
	 */
	public void warn(ILocation loc, String longDescription) {
		UltimatePreferenceStore prefs = new UltimatePreferenceStore(Activator.s_PLUGIN_ID);
		boolean reportUnsoundnessWarning = prefs
				.getBoolean(CACSLPreferenceInitializer.LABEL_REPORT_UNSOUNDNESS_WARNING);
		if (reportUnsoundnessWarning) {
			String shortDescription = "Unsoundness Warning";
			mLogger.warn(shortDescription + " " + longDescription);
			GenericResultAtLocation result = new GenericResultAtLocation(Activator.s_PLUGIN_NAME, loc,
					shortDescription, longDescription, Severity.WARNING);
			mServices.getResultService().reportResult(Activator.s_PLUGIN_ID, result);
		}
	}

	/**
	 * Getter for the setting: checked method.
	 * 
	 * @return the checked method's name.
	 */
	public String getCheckedMethod() {
		UltimatePreferenceStore prefs = new UltimatePreferenceStore(Activator.s_PLUGIN_ID);
		String checkMethod = SFO.EMPTY;
		try {
			checkMethod = prefs.getString(CACSLPreferenceInitializer.LABEL_MAINPROC);
		} catch (Exception e) {
			throw new IllegalArgumentException("Unable to determine specified checked method.");
		}
		return checkMethod;
	}

	/**
	 * Whether the memory model is required or not.
	 * 
	 * @return whether the memory model is required or not.
	 */
	public abstract boolean isMMRequired();

	/**
	 * Getter for the identifier mapping.
	 * 
	 * @return the mapping of Boogie identifiers to origin C identifiers.
	 */
	public Map<String, String> getIdentifierMapping() {
		return (cHandler.getSymbolTable().getIdentifierMapping());
	}

	public LinkedHashMap<String,Integer> getFunctionToIndex() {
		return mFunctionToIndex;
	}

	public TypeSizes getTypeSizes() {
		return m_TypeSizes;
	}
	
	public TranslationSettings getTranslationSettings() {
		return m_TranslationSettings;
	}

	public class TranslationSettings {
		private final POINTER_CHECKMODE m_DivisionByZero;

		public TranslationSettings(UltimatePreferenceStore preferences) {
			m_DivisionByZero = 
					preferences.getEnum(CACSLPreferenceInitializer.LABEL_CHECK_DIVISION_BY_ZERO, POINTER_CHECKMODE.class);
		}

		public POINTER_CHECKMODE getDivisionByZero() {
			return m_DivisionByZero;
		}
		
		
	}

	public abstract LinkedHashSet<IASTDeclaration> getReachableDeclarationsOrDeclarators();
	
	
}
