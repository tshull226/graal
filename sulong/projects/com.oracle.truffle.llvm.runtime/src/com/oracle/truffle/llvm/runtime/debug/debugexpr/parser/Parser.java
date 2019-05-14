package com.oracle.truffle.llvm.runtime.debug.debugexpr.parser;

import com.oracle.truffle.api.TruffleLanguage.InlineParsingRequest;
import com.oracle.truffle.llvm.runtime.LLVMContext;
import com.oracle.truffle.llvm.runtime.debug.debugexpr.nodes.*;
// Set the name of your grammar here (and at the end of this grammar):

import java.util.List;

public class Parser {
	public static final int _EOF = 0;
	public static final int _ident = 1;
	public static final int _number = 2;
	public static final int _floatnumber = 3;
	public static final int _charConst = 4;
	public static final int _stringType = 5;
	public static final int maxT = 45;

	static final boolean T = true;
	static final boolean x = false;
	static final int minErrDist = 2;

	public Token t;    // last recognized token
	public Token la;   // lookahead token
	int errDist = minErrDist;
	
	public Scanner scanner;
	public Errors errors;

	//only for the editor
	private boolean isContentAssistant = false;
	private boolean errorsDetected = false;
	private boolean updateProposals = false;

	private int stopPosition = 0;
	private int proposalToken = _EOF;
	private List<String> ccSymbols = null;
	private List<String> proposals = null;
	private Token dummy = new Token();


	boolean IsCast() {
	return false;
}

DebugExprSymbolTable tab;
InlineParsingRequest inlineParsingRequest;
DebugExprOperandNode operand=null;
LLVMContext context=null;

void SetSymtab(DebugExprSymbolTable tab) {
	this.tab = tab;
}

void SetContext(LLVMContext context) {
	this.context = context;
}

void SetParsingRequest(InlineParsingRequest inlineParsingRequest) {
	this.inlineParsingRequest=inlineParsingRequest;
}

public int GetErrors() {
	return errors.count;
}

public DebugExprOperandNode GetOperand() {return operand; }
public DebugExprNodeFactory NF() {return DebugExprNodeFactory.Get(); }
// If you want your generated compiler case insensitive add the
// keyword IGNORECASE here.




	public Parser(Scanner scanner) {
		this.scanner = scanner;
		errors = new Errors();
	}

	void SynErr (int n) {
		if (errDist >= minErrDist) errors.SynErr(la.line, la.col, n);
		errDist = 0;
		
		//for the editor
		errorsDetected = true;
	}

	public void SemErr (String msg) {
		if (errDist >= minErrDist) errors.SemErr(t.line, t.col, msg);
		errDist = 0;

		//for the editor
		errorsDetected = true;
	}
	 
	void Get () {
		if(isContentAssistant && updateProposals){
			la = la.next;
			if(!errorsDetected){
				proposals = ccSymbols;
			
				errorsDetected = true;
			}
		}
		
		else{
			for (;;) {
				t = la;
				la = scanner.Scan();
				if (la.kind <= maxT) {
					++errDist;
					break;
				} 

				la = t;

			}
		}
				

		

		//auch aktuellen token mitgeben,
		//if la.charPos >= current Token && la.charPos < stopPosition + la.val.length()
		//  Token temp = la.clone();
		//	la.kind = proposalToken;


		//only for the Editor
		if(isContentAssistant && !errorsDetected && la.charPos >= stopPosition + la.val.length()){
			dummy = createDummy();
			dummy.next = la;
			la = dummy;
			updateProposals = true;
			
		}
		ccSymbols = null;
	}
	
	void Expect (int n) {
		if (la.kind==n) Get(); else { SynErr(n); }
	}
	
	boolean StartOf (int s) {
		return set[s][la.kind];
	}
	
	void ExpectWeak (int n, int follow) {
		if (la.kind == n) Get();
		else {
			SynErr(n);
			while (!StartOf(follow)) Get();
		}
	}
	
	boolean WeakSeparator (int n, int syFol, int repFol) {
		int kind = la.kind;
		if (kind == n) { Get(); return true; }
		else if (StartOf(repFol)) return false;
		else {
			SynErr(n);
			while (!(set[syFol][kind] || set[repFol][kind] || set[0][kind])) {
				Get();
				kind = la.kind;
			}
			return StartOf(syFol);
		}
	}
	
	void DebugExpr() {
		DebugExprOperandNode n=null; 
		n = Expr();
		operand =n; 
	}

	DebugExprOperandNode  Expr() {
		DebugExprOperandNode  n;
		DebugExprOperandNode nThen=null, nElse=null; 
		n = LogOrExpr();
		if (la.kind == 34) {
			Get();
			nThen = Expr();
			Expect(35);
			nElse = Expr();
		}
		return n;
	}

	DebugExprOperandNode  PrimExpr() {
		DebugExprOperandNode  n;
		n=null; 
		switch (la.kind) {
		case 1: {
			Get();
			n = NF().createTabNode(tab.find(t.val));	
			break;
		}
		case 2: {
			Get();
			n = NF().createIntNode(Integer.parseInt(t.val)); 
			break;
		}
		case 3: {
			Get();
			n = NF().createFloatNode(Float.parseFloat(t.val)); 
			break;
		}
		case 4: {
			Get();
			n = NF().createStringNode(t.val); 
			break;
		}
		case 5: {
			Get();
			n = NF().createStringNode(t.val); 
			break;
		}
		case 6: {
			Get();
			n = Expr();
			Expect(7);
			break;
		}
		default: SynErr(46); break;
		}
		return n;
	}

	DebugExprOperandNode  Designator() {
		DebugExprOperandNode  n;
		DebugExprOperandNode idx=null; 
		n = PrimExpr();
		while (StartOf(1)) {
			if (la.kind == 8) {
				Get();
				idx = Expr();
				Expect(9);
			} else if (la.kind == 6) {
				ActPars();
			} else if (la.kind == 10) {
				Get();
				Expect(1);
			} else {
				Get();
				Expect(1);
			}
		}
		return n;
	}

	void ActPars() {
		DebugExprOperandNode n1=null, n2=null; 
		Expect(6);
		if (StartOf(2)) {
			n1 = Expr();
			while (la.kind == 12) {
				Get();
				n2 = Expr();
			}
		}
		Expect(7);
	}

	DebugExprOperandNode  UnaryExpr() {
		DebugExprOperandNode  n;
		n=null; 
		if (StartOf(3)) {
			n = Designator();
		} else if (StartOf(4)) {
			UnaryOp();
			n = CastExpr();
		} else if (la.kind == 13) {
			Get();
			Expect(6);
			Type();
			Expect(7);
		} else SynErr(47);
		return n;
	}

	void UnaryOp() {
		switch (la.kind) {
		case 14: {
			Get();
			break;
		}
		case 15: {
			Get();
			break;
		}
		case 16: {
			Get();
			break;
		}
		case 17: {
			Get();
			break;
		}
		case 18: {
			Get();
			break;
		}
		case 19: {
			Get();
			break;
		}
		default: SynErr(48); break;
		}
	}

	DebugExprOperandNode  CastExpr() {
		DebugExprOperandNode  n;
		Object typeO=null; 
		if (IsCast()) {
			Expect(6);
			Type();
			Expect(7);
		}
		n = UnaryExpr();
		if(typeO!=null) n.type = typeO; 
		return n;
	}

	void Type() {
		BaseType();
		if (la.kind == 7 || la.kind == 8 || la.kind == 15) {
			while (la.kind == 15) {
				Get();
			}
		} else if (la.kind == 14) {
			Get();
		} else SynErr(49);
		while (la.kind == 8) {
			Get();
			if (la.kind == 2) {
				Get();
			}
			Expect(9);
		}
	}

	DebugExprOperandNode  MultExpr() {
		DebugExprOperandNode  n;
		DebugExprOperandNode n1=null; 
		n = CastExpr();
		while (la.kind == 15 || la.kind == 20 || la.kind == 21) {
			if (la.kind == 15) {
				Get();
				n1 = CastExpr();
				n = NF().createMulNode(n, n1); System.out.println(n); 
			} else if (la.kind == 20) {
				Get();
				n1 = CastExpr();
				n = NF().createDivNode(n, n1); System.out.println(n); 
			} else {
				Get();
				n1 = CastExpr();
				n = NF().createRemNode(n, n1); System.out.println(n); 
			}
		}
		return n;
	}

	DebugExprOperandNode  AddExpr() {
		DebugExprOperandNode  n;
		DebugExprOperandNode n1=null; 
		n = MultExpr();
		while (la.kind == 16 || la.kind == 17) {
			if (la.kind == 16) {
				Get();
				n1 = MultExpr();
				n = NF().createAddNode(n, n1, context); System.out.println(n); 
			} else {
				Get();
				n1 = MultExpr();
				n = NF().createSubNode(n, n1); System.out.println(n); 
			}
		}
		return n;
	}

	DebugExprOperandNode  ShiftExpr() {
		DebugExprOperandNode  n;
		DebugExprOperandNode n1=null; 
		n = AddExpr();
		while (la.kind == 22 || la.kind == 23) {
			if (la.kind == 22) {
				Get();
				n1 = AddExpr();
			} else {
				Get();
				n1 = AddExpr();
			}
		}
		return n;
	}

	DebugExprOperandNode  RelExpr() {
		DebugExprOperandNode  n;
		DebugExprOperandNode n1=null; 
		n = ShiftExpr();
		while (StartOf(5)) {
			if (la.kind == 24) {
				Get();
				n1 = ShiftExpr();
			} else if (la.kind == 25) {
				Get();
				n1 = ShiftExpr();
			} else if (la.kind == 26) {
				Get();
				n1 = ShiftExpr();
			} else {
				Get();
				n1 = ShiftExpr();
			}
		}
		return n;
	}

	DebugExprOperandNode  EqExpr() {
		DebugExprOperandNode  n;
		DebugExprOperandNode n1=null; 
		n = RelExpr();
		while (la.kind == 28 || la.kind == 29) {
			if (la.kind == 28) {
				Get();
				n1 = RelExpr();
			} else {
				Get();
				n1 = RelExpr();
			}
		}
		return n;
	}

	DebugExprOperandNode  AndExpr() {
		DebugExprOperandNode  n;
		DebugExprOperandNode n1=null; 
		n = EqExpr();
		while (la.kind == 14) {
			Get();
			n1 = EqExpr();
		}
		return n;
	}

	DebugExprOperandNode  XorExpr() {
		DebugExprOperandNode  n;
		DebugExprOperandNode n1=null; 
		n = AndExpr();
		while (la.kind == 30) {
			Get();
			n1 = AndExpr();
		}
		return n;
	}

	DebugExprOperandNode  OrExpr() {
		DebugExprOperandNode  n;
		DebugExprOperandNode n1=null; 
		n = XorExpr();
		while (la.kind == 31) {
			Get();
			n1 = XorExpr();
		}
		return n;
	}

	DebugExprOperandNode  LogAndExpr() {
		DebugExprOperandNode  n;
		DebugExprOperandNode n1=null; 
		n = OrExpr();
		while (la.kind == 32) {
			Get();
			n1 = OrExpr();
		}
		return n;
	}

	DebugExprOperandNode  LogOrExpr() {
		DebugExprOperandNode  n;
		DebugExprOperandNode n1=null; 
		n = LogAndExpr();
		while (la.kind == 33) {
			Get();
			n1 = LogAndExpr();
		}
		return n;
	}

	void BaseType() {
		switch (la.kind) {
		case 36: {
			Get();
			break;
		}
		case 1: {
			Get();
			break;
		}
		case 37: case 38: {
			if (la.kind == 37) {
				Get();
			} else {
				Get();
			}
			if (StartOf(6)) {
				if (la.kind == 39) {
					Get();
				} else if (la.kind == 40) {
					Get();
				} else if (la.kind == 41) {
					Get();
				} else {
					Get();
				}
			}
			break;
		}
		case 39: {
			Get();
			break;
		}
		case 40: {
			Get();
			break;
		}
		case 41: {
			Get();
			break;
		}
		case 42: {
			Get();
			if (la.kind == 43) {
				Get();
			}
			break;
		}
		case 44: {
			Get();
			break;
		}
		case 43: {
			Get();
			break;
		}
		default: SynErr(50); break;
		}
	}



	public void Parse() {
		la = new Token();
		la.val = "";		
		Get();
		DebugExpr();
		Expect(0);

	}

	private static final boolean[][] set = {
		{T,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x},
		{x,x,x,x, x,x,T,x, T,x,T,T, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x},
		{x,T,T,T, T,T,T,x, x,x,x,x, x,T,T,T, T,T,T,T, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x},
		{x,T,T,T, T,T,T,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x},
		{x,x,x,x, x,x,x,x, x,x,x,x, x,x,T,T, T,T,T,T, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x},
		{x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, T,T,T,T, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x},
		{x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,T, T,T,T,x, x,x,x}

	};
 
	
	//only for the editor
	public Parser(Scanner scanner, int proposalToken, int stopPosition){
		this(scanner);
		isContentAssistant = true;
		this.proposalToken = proposalToken;
		this.stopPosition = stopPosition;
	}

	public String ParseErrors(){
		java.io.PrintStream oldStream = System.out;
		
		java.io.OutputStream out = new java.io.ByteArrayOutputStream();
		java.io.PrintStream newStream = new java.io.PrintStream(out);
		
		errors.errorStream = newStream;
				
		Parse();

		String errorStream = out.toString();
		errors.errorStream = oldStream;

		return errorStream;

	}

	public List<String> getCodeCompletionProposals(){
		return proposals;
	}

	private Token createDummy(){
		Token token = new Token();
		
		token.pos = la.pos;
		token.charPos = la.charPos;
		token.line = la.line;
		token.col = la.col;
		
		token.kind = proposalToken;
		token.val = "";
		
		
		return token;
	}
} // end Parser


class Errors {
	public int count = 0;                                    // number of errors detected
	public java.io.PrintStream errorStream = System.out;     // error messages go to this stream
	public String errMsgFormat = "-- line {0} col {1}: {2}"; // 0=line, 1=column, 2=text
	
	protected void printMsg(int line, int column, String msg) {
		StringBuffer b = new StringBuffer(errMsgFormat);
		int pos = b.indexOf("{0}");
		if (pos >= 0) { b.delete(pos, pos+3); b.insert(pos, line); }
		pos = b.indexOf("{1}");
		if (pos >= 0) { b.delete(pos, pos+3); b.insert(pos, column); }
		pos = b.indexOf("{2}");
		if (pos >= 0) b.replace(pos, pos+3, msg);
		errorStream.println(b.toString());
	}
	
	public void SynErr (int line, int col, int n) {
		String s;
		switch (n) {
			case 0: s = "EOF expected"; break;
			case 1: s = "ident expected"; break;
			case 2: s = "number expected"; break;
			case 3: s = "floatnumber expected"; break;
			case 4: s = "charConst expected"; break;
			case 5: s = "stringType expected"; break;
			case 6: s = "\"(\" expected"; break;
			case 7: s = "\")\" expected"; break;
			case 8: s = "\"[\" expected"; break;
			case 9: s = "\"]\" expected"; break;
			case 10: s = "\".\" expected"; break;
			case 11: s = "\"->\" expected"; break;
			case 12: s = "\",\" expected"; break;
			case 13: s = "\"sizeof\" expected"; break;
			case 14: s = "\"&\" expected"; break;
			case 15: s = "\"*\" expected"; break;
			case 16: s = "\"+\" expected"; break;
			case 17: s = "\"-\" expected"; break;
			case 18: s = "\"~\" expected"; break;
			case 19: s = "\"!\" expected"; break;
			case 20: s = "\"/\" expected"; break;
			case 21: s = "\"%\" expected"; break;
			case 22: s = "\"<<\" expected"; break;
			case 23: s = "\">>\" expected"; break;
			case 24: s = "\"<\" expected"; break;
			case 25: s = "\">\" expected"; break;
			case 26: s = "\"<=\" expected"; break;
			case 27: s = "\">=\" expected"; break;
			case 28: s = "\"==\" expected"; break;
			case 29: s = "\"!=\" expected"; break;
			case 30: s = "\"^\" expected"; break;
			case 31: s = "\"|\" expected"; break;
			case 32: s = "\"&&\" expected"; break;
			case 33: s = "\"||\" expected"; break;
			case 34: s = "\"?\" expected"; break;
			case 35: s = "\":\" expected"; break;
			case 36: s = "\"void\" expected"; break;
			case 37: s = "\"signed\" expected"; break;
			case 38: s = "\"unsigned\" expected"; break;
			case 39: s = "\"char\" expected"; break;
			case 40: s = "\"short\" expected"; break;
			case 41: s = "\"int\" expected"; break;
			case 42: s = "\"long\" expected"; break;
			case 43: s = "\"double\" expected"; break;
			case 44: s = "\"float\" expected"; break;
			case 45: s = "??? expected"; break;
			case 46: s = "invalid PrimExpr"; break;
			case 47: s = "invalid UnaryExpr"; break;
			case 48: s = "invalid UnaryOp"; break;
			case 49: s = "invalid Type"; break;
			case 50: s = "invalid BaseType"; break;
			default: s = "error " + n; break;
		}
		printMsg(line, col, s);
		count++;
	}

	public void SemErr (int line, int col, String s) {	
		printMsg(line, col, s);
		count++;
	}
	
	public void SemErr (String s) {
		errorStream.println(s);
		count++;
	}
	
	public void Warning (int line, int col, String s) {	
		printMsg(line, col, s);
	}
	
	public void Warning (String s) {
		errorStream.println(s);
	}
} // Errors


class FatalError extends RuntimeException {
	public static final long serialVersionUID = 1L;
	public FatalError(String s) { super(s); }
}
