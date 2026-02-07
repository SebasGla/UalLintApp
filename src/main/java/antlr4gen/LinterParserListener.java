// Generated from C:/Users/sebas/UalLintApp/grammar/LinterParser.g4 by ANTLR 4.13.2

package antlr4gen;

import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link LinterParser}.
 */
public interface LinterParserListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link LinterParser#program}.
	 * @param ctx the parse tree
	 */
	void enterProgram(LinterParser.ProgramContext ctx);
	/**
	 * Exit a parse tree produced by {@link LinterParser#program}.
	 * @param ctx the parse tree
	 */
	void exitProgram(LinterParser.ProgramContext ctx);
	/**
	 * Enter a parse tree produced by {@link LinterParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterStatement(LinterParser.StatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link LinterParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitStatement(LinterParser.StatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link LinterParser#instruction}.
	 * @param ctx the parse tree
	 */
	void enterInstruction(LinterParser.InstructionContext ctx);
	/**
	 * Exit a parse tree produced by {@link LinterParser#instruction}.
	 * @param ctx the parse tree
	 */
	void exitInstruction(LinterParser.InstructionContext ctx);
	/**
	 * Enter a parse tree produced by {@link LinterParser#routine}.
	 * @param ctx the parse tree
	 */
	void enterRoutine(LinterParser.RoutineContext ctx);
	/**
	 * Exit a parse tree produced by {@link LinterParser#routine}.
	 * @param ctx the parse tree
	 */
	void exitRoutine(LinterParser.RoutineContext ctx);
	/**
	 * Enter a parse tree produced by the {@code RoutinePopThenBx}
	 * labeled alternative in {@link LinterParser#routineBody}.
	 * @param ctx the parse tree
	 */
	void enterRoutinePopThenBx(LinterParser.RoutinePopThenBxContext ctx);
	/**
	 * Exit a parse tree produced by the {@code RoutinePopThenBx}
	 * labeled alternative in {@link LinterParser#routineBody}.
	 * @param ctx the parse tree
	 */
	void exitRoutinePopThenBx(LinterParser.RoutinePopThenBxContext ctx);
	/**
	 * Enter a parse tree produced by the {@code RoutinePopOnly}
	 * labeled alternative in {@link LinterParser#routineBody}.
	 * @param ctx the parse tree
	 */
	void enterRoutinePopOnly(LinterParser.RoutinePopOnlyContext ctx);
	/**
	 * Exit a parse tree produced by the {@code RoutinePopOnly}
	 * labeled alternative in {@link LinterParser#routineBody}.
	 * @param ctx the parse tree
	 */
	void exitRoutinePopOnly(LinterParser.RoutinePopOnlyContext ctx);
	/**
	 * Enter a parse tree produced by the {@code RoutineBxOnly}
	 * labeled alternative in {@link LinterParser#routineBody}.
	 * @param ctx the parse tree
	 */
	void enterRoutineBxOnly(LinterParser.RoutineBxOnlyContext ctx);
	/**
	 * Exit a parse tree produced by the {@code RoutineBxOnly}
	 * labeled alternative in {@link LinterParser#routineBody}.
	 * @param ctx the parse tree
	 */
	void exitRoutineBxOnly(LinterParser.RoutineBxOnlyContext ctx);
	/**
	 * Enter a parse tree produced by {@link LinterParser#routineBlock}.
	 * @param ctx the parse tree
	 */
	void enterRoutineBlock(LinterParser.RoutineBlockContext ctx);
	/**
	 * Exit a parse tree produced by {@link LinterParser#routineBlock}.
	 * @param ctx the parse tree
	 */
	void exitRoutineBlock(LinterParser.RoutineBlockContext ctx);
	/**
	 * Enter a parse tree produced by {@link LinterParser#push}.
	 * @param ctx the parse tree
	 */
	void enterPush(LinterParser.PushContext ctx);
	/**
	 * Exit a parse tree produced by {@link LinterParser#push}.
	 * @param ctx the parse tree
	 */
	void exitPush(LinterParser.PushContext ctx);
	/**
	 * Enter a parse tree produced by {@link LinterParser#pop}.
	 * @param ctx the parse tree
	 */
	void enterPop(LinterParser.PopContext ctx);
	/**
	 * Exit a parse tree produced by {@link LinterParser#pop}.
	 * @param ctx the parse tree
	 */
	void exitPop(LinterParser.PopContext ctx);
	/**
	 * Enter a parse tree produced by {@link LinterParser#routineBX}.
	 * @param ctx the parse tree
	 */
	void enterRoutineBX(LinterParser.RoutineBXContext ctx);
	/**
	 * Exit a parse tree produced by {@link LinterParser#routineBX}.
	 * @param ctx the parse tree
	 */
	void exitRoutineBX(LinterParser.RoutineBXContext ctx);
	/**
	 * Enter a parse tree produced by {@link LinterParser#regList}.
	 * @param ctx the parse tree
	 */
	void enterRegList(LinterParser.RegListContext ctx);
	/**
	 * Exit a parse tree produced by {@link LinterParser#regList}.
	 * @param ctx the parse tree
	 */
	void exitRegList(LinterParser.RegListContext ctx);
	/**
	 * Enter a parse tree produced by {@link LinterParser#regElem}.
	 * @param ctx the parse tree
	 */
	void enterRegElem(LinterParser.RegElemContext ctx);
	/**
	 * Exit a parse tree produced by {@link LinterParser#regElem}.
	 * @param ctx the parse tree
	 */
	void exitRegElem(LinterParser.RegElemContext ctx);
	/**
	 * Enter a parse tree produced by {@link LinterParser#reptBlock}.
	 * @param ctx the parse tree
	 */
	void enterReptBlock(LinterParser.ReptBlockContext ctx);
	/**
	 * Exit a parse tree produced by {@link LinterParser#reptBlock}.
	 * @param ctx the parse tree
	 */
	void exitReptBlock(LinterParser.ReptBlockContext ctx);
	/**
	 * Enter a parse tree produced by {@link LinterParser#variableDecl}.
	 * @param ctx the parse tree
	 */
	void enterVariableDecl(LinterParser.VariableDeclContext ctx);
	/**
	 * Exit a parse tree produced by {@link LinterParser#variableDecl}.
	 * @param ctx the parse tree
	 */
	void exitVariableDecl(LinterParser.VariableDeclContext ctx);
	/**
	 * Enter a parse tree produced by the {@code ArithmLong}
	 * labeled alternative in {@link LinterParser#arithmeticInstr}.
	 * @param ctx the parse tree
	 */
	void enterArithmLong(LinterParser.ArithmLongContext ctx);
	/**
	 * Exit a parse tree produced by the {@code ArithmLong}
	 * labeled alternative in {@link LinterParser#arithmeticInstr}.
	 * @param ctx the parse tree
	 */
	void exitArithmLong(LinterParser.ArithmLongContext ctx);
	/**
	 * Enter a parse tree produced by the {@code ArithmShort}
	 * labeled alternative in {@link LinterParser#arithmeticInstr}.
	 * @param ctx the parse tree
	 */
	void enterArithmShort(LinterParser.ArithmShortContext ctx);
	/**
	 * Exit a parse tree produced by the {@code ArithmShort}
	 * labeled alternative in {@link LinterParser#arithmeticInstr}.
	 * @param ctx the parse tree
	 */
	void exitArithmShort(LinterParser.ArithmShortContext ctx);
	/**
	 * Enter a parse tree produced by the {@code MulLong}
	 * labeled alternative in {@link LinterParser#mulInstr}.
	 * @param ctx the parse tree
	 */
	void enterMulLong(LinterParser.MulLongContext ctx);
	/**
	 * Exit a parse tree produced by the {@code MulLong}
	 * labeled alternative in {@link LinterParser#mulInstr}.
	 * @param ctx the parse tree
	 */
	void exitMulLong(LinterParser.MulLongContext ctx);
	/**
	 * Enter a parse tree produced by the {@code MulShort}
	 * labeled alternative in {@link LinterParser#mulInstr}.
	 * @param ctx the parse tree
	 */
	void enterMulShort(LinterParser.MulShortContext ctx);
	/**
	 * Exit a parse tree produced by the {@code MulShort}
	 * labeled alternative in {@link LinterParser#mulInstr}.
	 * @param ctx the parse tree
	 */
	void exitMulShort(LinterParser.MulShortContext ctx);
	/**
	 * Enter a parse tree produced by {@link LinterParser#mulASInstr}.
	 * @param ctx the parse tree
	 */
	void enterMulASInstr(LinterParser.MulASInstrContext ctx);
	/**
	 * Exit a parse tree produced by {@link LinterParser#mulASInstr}.
	 * @param ctx the parse tree
	 */
	void exitMulASInstr(LinterParser.MulASInstrContext ctx);
	/**
	 * Enter a parse tree produced by {@link LinterParser#adrInstr}.
	 * @param ctx the parse tree
	 */
	void enterAdrInstr(LinterParser.AdrInstrContext ctx);
	/**
	 * Exit a parse tree produced by {@link LinterParser#adrInstr}.
	 * @param ctx the parse tree
	 */
	void exitAdrInstr(LinterParser.AdrInstrContext ctx);
	/**
	 * Enter a parse tree produced by {@link LinterParser#movInstr}.
	 * @param ctx the parse tree
	 */
	void enterMovInstr(LinterParser.MovInstrContext ctx);
	/**
	 * Exit a parse tree produced by {@link LinterParser#movInstr}.
	 * @param ctx the parse tree
	 */
	void exitMovInstr(LinterParser.MovInstrContext ctx);
	/**
	 * Enter a parse tree produced by {@link LinterParser#movtInstr}.
	 * @param ctx the parse tree
	 */
	void enterMovtInstr(LinterParser.MovtInstrContext ctx);
	/**
	 * Exit a parse tree produced by {@link LinterParser#movtInstr}.
	 * @param ctx the parse tree
	 */
	void exitMovtInstr(LinterParser.MovtInstrContext ctx);
	/**
	 * Enter a parse tree produced by {@link LinterParser#mov32Instr}.
	 * @param ctx the parse tree
	 */
	void enterMov32Instr(LinterParser.Mov32InstrContext ctx);
	/**
	 * Exit a parse tree produced by {@link LinterParser#mov32Instr}.
	 * @param ctx the parse tree
	 */
	void exitMov32Instr(LinterParser.Mov32InstrContext ctx);
	/**
	 * Enter a parse tree produced by {@link LinterParser#branch}.
	 * @param ctx the parse tree
	 */
	void enterBranch(LinterParser.BranchContext ctx);
	/**
	 * Exit a parse tree produced by {@link LinterParser#branch}.
	 * @param ctx the parse tree
	 */
	void exitBranch(LinterParser.BranchContext ctx);
	/**
	 * Enter a parse tree produced by {@link LinterParser#bkptInstr}.
	 * @param ctx the parse tree
	 */
	void enterBkptInstr(LinterParser.BkptInstrContext ctx);
	/**
	 * Exit a parse tree produced by {@link LinterParser#bkptInstr}.
	 * @param ctx the parse tree
	 */
	void exitBkptInstr(LinterParser.BkptInstrContext ctx);
	/**
	 * Enter a parse tree produced by {@link LinterParser#cpsInstr}.
	 * @param ctx the parse tree
	 */
	void enterCpsInstr(LinterParser.CpsInstrContext ctx);
	/**
	 * Exit a parse tree produced by {@link LinterParser#cpsInstr}.
	 * @param ctx the parse tree
	 */
	void exitCpsInstr(LinterParser.CpsInstrContext ctx);
	/**
	 * Enter a parse tree produced by {@link LinterParser#cbzInstr}.
	 * @param ctx the parse tree
	 */
	void enterCbzInstr(LinterParser.CbzInstrContext ctx);
	/**
	 * Exit a parse tree produced by {@link LinterParser#cbzInstr}.
	 * @param ctx the parse tree
	 */
	void exitCbzInstr(LinterParser.CbzInstrContext ctx);
	/**
	 * Enter a parse tree produced by {@link LinterParser#compInstr}.
	 * @param ctx the parse tree
	 */
	void enterCompInstr(LinterParser.CompInstrContext ctx);
	/**
	 * Exit a parse tree produced by {@link LinterParser#compInstr}.
	 * @param ctx the parse tree
	 */
	void exitCompInstr(LinterParser.CompInstrContext ctx);
	/**
	 * Enter a parse tree produced by {@link LinterParser#ldrInstr}.
	 * @param ctx the parse tree
	 */
	void enterLdrInstr(LinterParser.LdrInstrContext ctx);
	/**
	 * Exit a parse tree produced by {@link LinterParser#ldrInstr}.
	 * @param ctx the parse tree
	 */
	void exitLdrInstr(LinterParser.LdrInstrContext ctx);
	/**
	 * Enter a parse tree produced by {@link LinterParser#strInstr}.
	 * @param ctx the parse tree
	 */
	void enterStrInstr(LinterParser.StrInstrContext ctx);
	/**
	 * Exit a parse tree produced by {@link LinterParser#strInstr}.
	 * @param ctx the parse tree
	 */
	void exitStrInstr(LinterParser.StrInstrContext ctx);
	/**
	 * Enter a parse tree produced by {@link LinterParser#ldrdInstr}.
	 * @param ctx the parse tree
	 */
	void enterLdrdInstr(LinterParser.LdrdInstrContext ctx);
	/**
	 * Exit a parse tree produced by {@link LinterParser#ldrdInstr}.
	 * @param ctx the parse tree
	 */
	void exitLdrdInstr(LinterParser.LdrdInstrContext ctx);
	/**
	 * Enter a parse tree produced by {@link LinterParser#strdInstr}.
	 * @param ctx the parse tree
	 */
	void enterStrdInstr(LinterParser.StrdInstrContext ctx);
	/**
	 * Exit a parse tree produced by {@link LinterParser#strdInstr}.
	 * @param ctx the parse tree
	 */
	void exitStrdInstr(LinterParser.StrdInstrContext ctx);
	/**
	 * Enter a parse tree produced by {@link LinterParser#ldrtInstr}.
	 * @param ctx the parse tree
	 */
	void enterLdrtInstr(LinterParser.LdrtInstrContext ctx);
	/**
	 * Exit a parse tree produced by {@link LinterParser#ldrtInstr}.
	 * @param ctx the parse tree
	 */
	void exitLdrtInstr(LinterParser.LdrtInstrContext ctx);
	/**
	 * Enter a parse tree produced by {@link LinterParser#strtInstr}.
	 * @param ctx the parse tree
	 */
	void enterStrtInstr(LinterParser.StrtInstrContext ctx);
	/**
	 * Exit a parse tree produced by {@link LinterParser#strtInstr}.
	 * @param ctx the parse tree
	 */
	void exitStrtInstr(LinterParser.StrtInstrContext ctx);
	/**
	 * Enter a parse tree produced by {@link LinterParser#itInstr}.
	 * @param ctx the parse tree
	 */
	void enterItInstr(LinterParser.ItInstrContext ctx);
	/**
	 * Exit a parse tree produced by {@link LinterParser#itInstr}.
	 * @param ctx the parse tree
	 */
	void exitItInstr(LinterParser.ItInstrContext ctx);
	/**
	 * Enter a parse tree produced by {@link LinterParser#shift}.
	 * @param ctx the parse tree
	 */
	void enterShift(LinterParser.ShiftContext ctx);
	/**
	 * Exit a parse tree produced by {@link LinterParser#shift}.
	 * @param ctx the parse tree
	 */
	void exitShift(LinterParser.ShiftContext ctx);
	/**
	 * Enter a parse tree produced by the {@code ShiftLong}
	 * labeled alternative in {@link LinterParser#shiftInstruction}.
	 * @param ctx the parse tree
	 */
	void enterShiftLong(LinterParser.ShiftLongContext ctx);
	/**
	 * Exit a parse tree produced by the {@code ShiftLong}
	 * labeled alternative in {@link LinterParser#shiftInstruction}.
	 * @param ctx the parse tree
	 */
	void exitShiftLong(LinterParser.ShiftLongContext ctx);
	/**
	 * Enter a parse tree produced by the {@code ShiftShort}
	 * labeled alternative in {@link LinterParser#shiftInstruction}.
	 * @param ctx the parse tree
	 */
	void enterShiftShort(LinterParser.ShiftShortContext ctx);
	/**
	 * Exit a parse tree produced by the {@code ShiftShort}
	 * labeled alternative in {@link LinterParser#shiftInstruction}.
	 * @param ctx the parse tree
	 */
	void exitShiftShort(LinterParser.ShiftShortContext ctx);
	/**
	 * Enter a parse tree produced by {@link LinterParser#rrxIntrstuction}.
	 * @param ctx the parse tree
	 */
	void enterRrxIntrstuction(LinterParser.RrxIntrstuctionContext ctx);
	/**
	 * Exit a parse tree produced by {@link LinterParser#rrxIntrstuction}.
	 * @param ctx the parse tree
	 */
	void exitRrxIntrstuction(LinterParser.RrxIntrstuctionContext ctx);
	/**
	 * Enter a parse tree produced by {@link LinterParser#bfxInstr}.
	 * @param ctx the parse tree
	 */
	void enterBfxInstr(LinterParser.BfxInstrContext ctx);
	/**
	 * Exit a parse tree produced by {@link LinterParser#bfxInstr}.
	 * @param ctx the parse tree
	 */
	void exitBfxInstr(LinterParser.BfxInstrContext ctx);
	/**
	 * Enter a parse tree produced by {@link LinterParser#bfiInstr}.
	 * @param ctx the parse tree
	 */
	void enterBfiInstr(LinterParser.BfiInstrContext ctx);
	/**
	 * Exit a parse tree produced by {@link LinterParser#bfiInstr}.
	 * @param ctx the parse tree
	 */
	void exitBfiInstr(LinterParser.BfiInstrContext ctx);
	/**
	 * Enter a parse tree produced by {@link LinterParser#bfcInstr}.
	 * @param ctx the parse tree
	 */
	void enterBfcInstr(LinterParser.BfcInstrContext ctx);
	/**
	 * Exit a parse tree produced by {@link LinterParser#bfcInstr}.
	 * @param ctx the parse tree
	 */
	void exitBfcInstr(LinterParser.BfcInstrContext ctx);
	/**
	 * Enter a parse tree produced by {@link LinterParser#divInstr}.
	 * @param ctx the parse tree
	 */
	void enterDivInstr(LinterParser.DivInstrContext ctx);
	/**
	 * Exit a parse tree produced by {@link LinterParser#divInstr}.
	 * @param ctx the parse tree
	 */
	void exitDivInstr(LinterParser.DivInstrContext ctx);
	/**
	 * Enter a parse tree produced by {@link LinterParser#testInstr}.
	 * @param ctx the parse tree
	 */
	void enterTestInstr(LinterParser.TestInstrContext ctx);
	/**
	 * Exit a parse tree produced by {@link LinterParser#testInstr}.
	 * @param ctx the parse tree
	 */
	void exitTestInstr(LinterParser.TestInstrContext ctx);
	/**
	 * Enter a parse tree produced by {@link LinterParser#nopInstr}.
	 * @param ctx the parse tree
	 */
	void enterNopInstr(LinterParser.NopInstrContext ctx);
	/**
	 * Exit a parse tree produced by {@link LinterParser#nopInstr}.
	 * @param ctx the parse tree
	 */
	void exitNopInstr(LinterParser.NopInstrContext ctx);
	/**
	 * Enter a parse tree produced by {@link LinterParser#selInstr}.
	 * @param ctx the parse tree
	 */
	void enterSelInstr(LinterParser.SelInstrContext ctx);
	/**
	 * Exit a parse tree produced by {@link LinterParser#selInstr}.
	 * @param ctx the parse tree
	 */
	void exitSelInstr(LinterParser.SelInstrContext ctx);
	/**
	 * Enter a parse tree produced by {@link LinterParser#plInstr}.
	 * @param ctx the parse tree
	 */
	void enterPlInstr(LinterParser.PlInstrContext ctx);
	/**
	 * Exit a parse tree produced by {@link LinterParser#plInstr}.
	 * @param ctx the parse tree
	 */
	void exitPlInstr(LinterParser.PlInstrContext ctx);
	/**
	 * Enter a parse tree produced by {@link LinterParser#nondwordOption}.
	 * @param ctx the parse tree
	 */
	void enterNondwordOption(LinterParser.NondwordOptionContext ctx);
	/**
	 * Exit a parse tree produced by {@link LinterParser#nondwordOption}.
	 * @param ctx the parse tree
	 */
	void exitNondwordOption(LinterParser.NondwordOptionContext ctx);
	/**
	 * Enter a parse tree produced by {@link LinterParser#dwordOption}.
	 * @param ctx the parse tree
	 */
	void enterDwordOption(LinterParser.DwordOptionContext ctx);
	/**
	 * Exit a parse tree produced by {@link LinterParser#dwordOption}.
	 * @param ctx the parse tree
	 */
	void exitDwordOption(LinterParser.DwordOptionContext ctx);
	/**
	 * Enter a parse tree produced by {@link LinterParser#memOption2}.
	 * @param ctx the parse tree
	 */
	void enterMemOption2(LinterParser.MemOption2Context ctx);
	/**
	 * Exit a parse tree produced by {@link LinterParser#memOption2}.
	 * @param ctx the parse tree
	 */
	void exitMemOption2(LinterParser.MemOption2Context ctx);
	/**
	 * Enter a parse tree produced by {@link LinterParser#memOption3}.
	 * @param ctx the parse tree
	 */
	void enterMemOption3(LinterParser.MemOption3Context ctx);
	/**
	 * Exit a parse tree produced by {@link LinterParser#memOption3}.
	 * @param ctx the parse tree
	 */
	void exitMemOption3(LinterParser.MemOption3Context ctx);
	/**
	 * Enter a parse tree produced by {@link LinterParser#memOption4}.
	 * @param ctx the parse tree
	 */
	void enterMemOption4(LinterParser.MemOption4Context ctx);
	/**
	 * Exit a parse tree produced by {@link LinterParser#memOption4}.
	 * @param ctx the parse tree
	 */
	void exitMemOption4(LinterParser.MemOption4Context ctx);
	/**
	 * Enter a parse tree produced by {@link LinterParser#shiftOperand}.
	 * @param ctx the parse tree
	 */
	void enterShiftOperand(LinterParser.ShiftOperandContext ctx);
	/**
	 * Exit a parse tree produced by {@link LinterParser#shiftOperand}.
	 * @param ctx the parse tree
	 */
	void exitShiftOperand(LinterParser.ShiftOperandContext ctx);
	/**
	 * Enter a parse tree produced by {@link LinterParser#shiftOption}.
	 * @param ctx the parse tree
	 */
	void enterShiftOption(LinterParser.ShiftOptionContext ctx);
	/**
	 * Exit a parse tree produced by {@link LinterParser#shiftOption}.
	 * @param ctx the parse tree
	 */
	void exitShiftOption(LinterParser.ShiftOptionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code LogicLong}
	 * labeled alternative in {@link LinterParser#logicInstr}.
	 * @param ctx the parse tree
	 */
	void enterLogicLong(LinterParser.LogicLongContext ctx);
	/**
	 * Exit a parse tree produced by the {@code LogicLong}
	 * labeled alternative in {@link LinterParser#logicInstr}.
	 * @param ctx the parse tree
	 */
	void exitLogicLong(LinterParser.LogicLongContext ctx);
	/**
	 * Enter a parse tree produced by the {@code LogicShort}
	 * labeled alternative in {@link LinterParser#logicInstr}.
	 * @param ctx the parse tree
	 */
	void enterLogicShort(LinterParser.LogicShortContext ctx);
	/**
	 * Exit a parse tree produced by the {@code LogicShort}
	 * labeled alternative in {@link LinterParser#logicInstr}.
	 * @param ctx the parse tree
	 */
	void exitLogicShort(LinterParser.LogicShortContext ctx);
	/**
	 * Enter a parse tree produced by {@link LinterParser#clzInstr}.
	 * @param ctx the parse tree
	 */
	void enterClzInstr(LinterParser.ClzInstrContext ctx);
	/**
	 * Exit a parse tree produced by {@link LinterParser#clzInstr}.
	 * @param ctx the parse tree
	 */
	void exitClzInstr(LinterParser.ClzInstrContext ctx);
	/**
	 * Enter a parse tree produced by {@link LinterParser#parallelOpInstr}.
	 * @param ctx the parse tree
	 */
	void enterParallelOpInstr(LinterParser.ParallelOpInstrContext ctx);
	/**
	 * Exit a parse tree produced by {@link LinterParser#parallelOpInstr}.
	 * @param ctx the parse tree
	 */
	void exitParallelOpInstr(LinterParser.ParallelOpInstrContext ctx);
	/**
	 * Enter a parse tree produced by {@link LinterParser#ldrStrMultipleInstr}.
	 * @param ctx the parse tree
	 */
	void enterLdrStrMultipleInstr(LinterParser.LdrStrMultipleInstrContext ctx);
	/**
	 * Exit a parse tree produced by {@link LinterParser#ldrStrMultipleInstr}.
	 * @param ctx the parse tree
	 */
	void exitLdrStrMultipleInstr(LinterParser.LdrStrMultipleInstrContext ctx);
	/**
	 * Enter a parse tree produced by {@link LinterParser#dualMultInstr}.
	 * @param ctx the parse tree
	 */
	void enterDualMultInstr(LinterParser.DualMultInstrContext ctx);
	/**
	 * Exit a parse tree produced by {@link LinterParser#dualMultInstr}.
	 * @param ctx the parse tree
	 */
	void exitDualMultInstr(LinterParser.DualMultInstrContext ctx);
	/**
	 * Enter a parse tree produced by {@link LinterParser#dualAddInstr}.
	 * @param ctx the parse tree
	 */
	void enterDualAddInstr(LinterParser.DualAddInstrContext ctx);
	/**
	 * Exit a parse tree produced by {@link LinterParser#dualAddInstr}.
	 * @param ctx the parse tree
	 */
	void exitDualAddInstr(LinterParser.DualAddInstrContext ctx);
	/**
	 * Enter a parse tree produced by {@link LinterParser#saturating16Instr}.
	 * @param ctx the parse tree
	 */
	void enterSaturating16Instr(LinterParser.Saturating16InstrContext ctx);
	/**
	 * Exit a parse tree produced by {@link LinterParser#saturating16Instr}.
	 * @param ctx the parse tree
	 */
	void exitSaturating16Instr(LinterParser.Saturating16InstrContext ctx);
	/**
	 * Enter a parse tree produced by {@link LinterParser#saturatingInstr}.
	 * @param ctx the parse tree
	 */
	void enterSaturatingInstr(LinterParser.SaturatingInstrContext ctx);
	/**
	 * Exit a parse tree produced by {@link LinterParser#saturatingInstr}.
	 * @param ctx the parse tree
	 */
	void exitSaturatingInstr(LinterParser.SaturatingInstrContext ctx);
	/**
	 * Enter a parse tree produced by {@link LinterParser#smulxyInstr}.
	 * @param ctx the parse tree
	 */
	void enterSmulxyInstr(LinterParser.SmulxyInstrContext ctx);
	/**
	 * Exit a parse tree produced by {@link LinterParser#smulxyInstr}.
	 * @param ctx the parse tree
	 */
	void exitSmulxyInstr(LinterParser.SmulxyInstrContext ctx);
	/**
	 * Enter a parse tree produced by {@link LinterParser#smalxyInstr}.
	 * @param ctx the parse tree
	 */
	void enterSmalxyInstr(LinterParser.SmalxyInstrContext ctx);
	/**
	 * Exit a parse tree produced by {@link LinterParser#smalxyInstr}.
	 * @param ctx the parse tree
	 */
	void exitSmalxyInstr(LinterParser.SmalxyInstrContext ctx);
	/**
	 * Enter a parse tree produced by {@link LinterParser#smmulInstr}.
	 * @param ctx the parse tree
	 */
	void enterSmmulInstr(LinterParser.SmmulInstrContext ctx);
	/**
	 * Exit a parse tree produced by {@link LinterParser#smmulInstr}.
	 * @param ctx the parse tree
	 */
	void exitSmmulInstr(LinterParser.SmmulInstrContext ctx);
	/**
	 * Enter a parse tree produced by {@link LinterParser#smmlasInstr}.
	 * @param ctx the parse tree
	 */
	void enterSmmlasInstr(LinterParser.SmmlasInstrContext ctx);
	/**
	 * Exit a parse tree produced by {@link LinterParser#smmlasInstr}.
	 * @param ctx the parse tree
	 */
	void exitSmmlasInstr(LinterParser.SmmlasInstrContext ctx);
	/**
	 * Enter a parse tree produced by {@link LinterParser#extendInstr}.
	 * @param ctx the parse tree
	 */
	void enterExtendInstr(LinterParser.ExtendInstrContext ctx);
	/**
	 * Exit a parse tree produced by {@link LinterParser#extendInstr}.
	 * @param ctx the parse tree
	 */
	void exitExtendInstr(LinterParser.ExtendInstrContext ctx);
	/**
	 * Enter a parse tree produced by {@link LinterParser#extendAddInstr}.
	 * @param ctx the parse tree
	 */
	void enterExtendAddInstr(LinterParser.ExtendAddInstrContext ctx);
	/**
	 * Exit a parse tree produced by {@link LinterParser#extendAddInstr}.
	 * @param ctx the parse tree
	 */
	void exitExtendAddInstr(LinterParser.ExtendAddInstrContext ctx);
	/**
	 * Enter a parse tree produced by {@link LinterParser#eventInstr}.
	 * @param ctx the parse tree
	 */
	void enterEventInstr(LinterParser.EventInstrContext ctx);
	/**
	 * Exit a parse tree produced by {@link LinterParser#eventInstr}.
	 * @param ctx the parse tree
	 */
	void exitEventInstr(LinterParser.EventInstrContext ctx);
	/**
	 * Enter a parse tree produced by {@link LinterParser#movwInstr}.
	 * @param ctx the parse tree
	 */
	void enterMovwInstr(LinterParser.MovwInstrContext ctx);
	/**
	 * Exit a parse tree produced by {@link LinterParser#movwInstr}.
	 * @param ctx the parse tree
	 */
	void exitMovwInstr(LinterParser.MovwInstrContext ctx);
	/**
	 * Enter a parse tree produced by {@link LinterParser#negInstr}.
	 * @param ctx the parse tree
	 */
	void enterNegInstr(LinterParser.NegInstrContext ctx);
	/**
	 * Exit a parse tree produced by {@link LinterParser#negInstr}.
	 * @param ctx the parse tree
	 */
	void exitNegInstr(LinterParser.NegInstrContext ctx);
	/**
	 * Enter a parse tree produced by {@link LinterParser#tbbInstr}.
	 * @param ctx the parse tree
	 */
	void enterTbbInstr(LinterParser.TbbInstrContext ctx);
	/**
	 * Exit a parse tree produced by {@link LinterParser#tbbInstr}.
	 * @param ctx the parse tree
	 */
	void exitTbbInstr(LinterParser.TbbInstrContext ctx);
	/**
	 * Enter a parse tree produced by {@link LinterParser#tbhInstr}.
	 * @param ctx the parse tree
	 */
	void enterTbhInstr(LinterParser.TbhInstrContext ctx);
	/**
	 * Exit a parse tree produced by {@link LinterParser#tbhInstr}.
	 * @param ctx the parse tree
	 */
	void exitTbhInstr(LinterParser.TbhInstrContext ctx);
	/**
	 * Enter a parse tree produced by {@link LinterParser#packHalfWordInstr}.
	 * @param ctx the parse tree
	 */
	void enterPackHalfWordInstr(LinterParser.PackHalfWordInstrContext ctx);
	/**
	 * Exit a parse tree produced by {@link LinterParser#packHalfWordInstr}.
	 * @param ctx the parse tree
	 */
	void exitPackHalfWordInstr(LinterParser.PackHalfWordInstrContext ctx);
	/**
	 * Enter a parse tree produced by {@link LinterParser#reverseInstr}.
	 * @param ctx the parse tree
	 */
	void enterReverseInstr(LinterParser.ReverseInstrContext ctx);
	/**
	 * Exit a parse tree produced by {@link LinterParser#reverseInstr}.
	 * @param ctx the parse tree
	 */
	void exitReverseInstr(LinterParser.ReverseInstrContext ctx);
	/**
	 * Enter a parse tree produced by {@link LinterParser#longMultiplyInstr}.
	 * @param ctx the parse tree
	 */
	void enterLongMultiplyInstr(LinterParser.LongMultiplyInstrContext ctx);
	/**
	 * Exit a parse tree produced by {@link LinterParser#longMultiplyInstr}.
	 * @param ctx the parse tree
	 */
	void exitLongMultiplyInstr(LinterParser.LongMultiplyInstrContext ctx);
	/**
	 * Enter a parse tree produced by {@link LinterParser#checkArrayInstr}.
	 * @param ctx the parse tree
	 */
	void enterCheckArrayInstr(LinterParser.CheckArrayInstrContext ctx);
	/**
	 * Exit a parse tree produced by {@link LinterParser#checkArrayInstr}.
	 * @param ctx the parse tree
	 */
	void exitCheckArrayInstr(LinterParser.CheckArrayInstrContext ctx);
	/**
	 * Enter a parse tree produced by {@link LinterParser#clearExInstr}.
	 * @param ctx the parse tree
	 */
	void enterClearExInstr(LinterParser.ClearExInstrContext ctx);
	/**
	 * Exit a parse tree produced by {@link LinterParser#clearExInstr}.
	 * @param ctx the parse tree
	 */
	void exitClearExInstr(LinterParser.ClearExInstrContext ctx);
	/**
	 * Enter a parse tree produced by {@link LinterParser#debugInstr}.
	 * @param ctx the parse tree
	 */
	void enterDebugInstr(LinterParser.DebugInstrContext ctx);
	/**
	 * Exit a parse tree produced by {@link LinterParser#debugInstr}.
	 * @param ctx the parse tree
	 */
	void exitDebugInstr(LinterParser.DebugInstrContext ctx);
	/**
	 * Enter a parse tree produced by {@link LinterParser#dmbInstr}.
	 * @param ctx the parse tree
	 */
	void enterDmbInstr(LinterParser.DmbInstrContext ctx);
	/**
	 * Exit a parse tree produced by {@link LinterParser#dmbInstr}.
	 * @param ctx the parse tree
	 */
	void exitDmbInstr(LinterParser.DmbInstrContext ctx);
	/**
	 * Enter a parse tree produced by {@link LinterParser#rfeInstr}.
	 * @param ctx the parse tree
	 */
	void enterRfeInstr(LinterParser.RfeInstrContext ctx);
	/**
	 * Exit a parse tree produced by {@link LinterParser#rfeInstr}.
	 * @param ctx the parse tree
	 */
	void exitRfeInstr(LinterParser.RfeInstrContext ctx);
	/**
	 * Enter a parse tree produced by {@link LinterParser#setendInstr}.
	 * @param ctx the parse tree
	 */
	void enterSetendInstr(LinterParser.SetendInstrContext ctx);
	/**
	 * Exit a parse tree produced by {@link LinterParser#setendInstr}.
	 * @param ctx the parse tree
	 */
	void exitSetendInstr(LinterParser.SetendInstrContext ctx);
	/**
	 * Enter a parse tree produced by {@link LinterParser#smcInstr}.
	 * @param ctx the parse tree
	 */
	void enterSmcInstr(LinterParser.SmcInstrContext ctx);
	/**
	 * Exit a parse tree produced by {@link LinterParser#smcInstr}.
	 * @param ctx the parse tree
	 */
	void exitSmcInstr(LinterParser.SmcInstrContext ctx);
	/**
	 * Enter a parse tree produced by {@link LinterParser#smlalxyInstr}.
	 * @param ctx the parse tree
	 */
	void enterSmlalxyInstr(LinterParser.SmlalxyInstrContext ctx);
	/**
	 * Exit a parse tree produced by {@link LinterParser#smlalxyInstr}.
	 * @param ctx the parse tree
	 */
	void exitSmlalxyInstr(LinterParser.SmlalxyInstrContext ctx);
	/**
	 * Enter a parse tree produced by {@link LinterParser#dual16bitmulInstr}.
	 * @param ctx the parse tree
	 */
	void enterDual16bitmulInstr(LinterParser.Dual16bitmulInstrContext ctx);
	/**
	 * Exit a parse tree produced by {@link LinterParser#dual16bitmulInstr}.
	 * @param ctx the parse tree
	 */
	void exitDual16bitmulInstr(LinterParser.Dual16bitmulInstrContext ctx);
	/**
	 * Enter a parse tree produced by {@link LinterParser#smulwInstr}.
	 * @param ctx the parse tree
	 */
	void enterSmulwInstr(LinterParser.SmulwInstrContext ctx);
	/**
	 * Exit a parse tree produced by {@link LinterParser#smulwInstr}.
	 * @param ctx the parse tree
	 */
	void exitSmulwInstr(LinterParser.SmulwInstrContext ctx);
	/**
	 * Enter a parse tree produced by {@link LinterParser#smlawInstr}.
	 * @param ctx the parse tree
	 */
	void enterSmlawInstr(LinterParser.SmlawInstrContext ctx);
	/**
	 * Exit a parse tree produced by {@link LinterParser#smlawInstr}.
	 * @param ctx the parse tree
	 */
	void exitSmlawInstr(LinterParser.SmlawInstrContext ctx);
	/**
	 * Enter a parse tree produced by {@link LinterParser#svcInstr}.
	 * @param ctx the parse tree
	 */
	void enterSvcInstr(LinterParser.SvcInstrContext ctx);
	/**
	 * Exit a parse tree produced by {@link LinterParser#svcInstr}.
	 * @param ctx the parse tree
	 */
	void exitSvcInstr(LinterParser.SvcInstrContext ctx);
	/**
	 * Enter a parse tree produced by {@link LinterParser#umaalInstr}.
	 * @param ctx the parse tree
	 */
	void enterUmaalInstr(LinterParser.UmaalInstrContext ctx);
	/**
	 * Exit a parse tree produced by {@link LinterParser#umaalInstr}.
	 * @param ctx the parse tree
	 */
	void exitUmaalInstr(LinterParser.UmaalInstrContext ctx);
	/**
	 * Enter a parse tree produced by {@link LinterParser#usad8Instr}.
	 * @param ctx the parse tree
	 */
	void enterUsad8Instr(LinterParser.Usad8InstrContext ctx);
	/**
	 * Exit a parse tree produced by {@link LinterParser#usad8Instr}.
	 * @param ctx the parse tree
	 */
	void exitUsad8Instr(LinterParser.Usad8InstrContext ctx);
	/**
	 * Enter a parse tree produced by {@link LinterParser#usada8Instr}.
	 * @param ctx the parse tree
	 */
	void enterUsada8Instr(LinterParser.Usada8InstrContext ctx);
	/**
	 * Exit a parse tree produced by {@link LinterParser#usada8Instr}.
	 * @param ctx the parse tree
	 */
	void exitUsada8Instr(LinterParser.Usada8InstrContext ctx);
	/**
	 * Enter a parse tree produced by {@link LinterParser#register}.
	 * @param ctx the parse tree
	 */
	void enterRegister(LinterParser.RegisterContext ctx);
	/**
	 * Exit a parse tree produced by {@link LinterParser#register}.
	 * @param ctx the parse tree
	 */
	void exitRegister(LinterParser.RegisterContext ctx);
	/**
	 * Enter a parse tree produced by {@link LinterParser#labelDef}.
	 * @param ctx the parse tree
	 */
	void enterLabelDef(LinterParser.LabelDefContext ctx);
	/**
	 * Exit a parse tree produced by {@link LinterParser#labelDef}.
	 * @param ctx the parse tree
	 */
	void exitLabelDef(LinterParser.LabelDefContext ctx);
	/**
	 * Enter a parse tree produced by {@link LinterParser#labelRef}.
	 * @param ctx the parse tree
	 */
	void enterLabelRef(LinterParser.LabelRefContext ctx);
	/**
	 * Exit a parse tree produced by {@link LinterParser#labelRef}.
	 * @param ctx the parse tree
	 */
	void exitLabelRef(LinterParser.LabelRefContext ctx);
	/**
	 * Enter a parse tree produced by {@link LinterParser#equDirective}.
	 * @param ctx the parse tree
	 */
	void enterEquDirective(LinterParser.EquDirectiveContext ctx);
	/**
	 * Exit a parse tree produced by {@link LinterParser#equDirective}.
	 * @param ctx the parse tree
	 */
	void exitEquDirective(LinterParser.EquDirectiveContext ctx);
	/**
	 * Enter a parse tree produced by {@link LinterParser#datatype}.
	 * @param ctx the parse tree
	 */
	void enterDatatype(LinterParser.DatatypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link LinterParser#datatype}.
	 * @param ctx the parse tree
	 */
	void exitDatatype(LinterParser.DatatypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link LinterParser#dataOnlyDecl}.
	 * @param ctx the parse tree
	 */
	void enterDataOnlyDecl(LinterParser.DataOnlyDeclContext ctx);
	/**
	 * Exit a parse tree produced by {@link LinterParser#dataOnlyDecl}.
	 * @param ctx the parse tree
	 */
	void exitDataOnlyDecl(LinterParser.DataOnlyDeclContext ctx);
	/**
	 * Enter a parse tree produced by {@link LinterParser#genericDirective}.
	 * @param ctx the parse tree
	 */
	void enterGenericDirective(LinterParser.GenericDirectiveContext ctx);
	/**
	 * Exit a parse tree produced by {@link LinterParser#genericDirective}.
	 * @param ctx the parse tree
	 */
	void exitGenericDirective(LinterParser.GenericDirectiveContext ctx);
	/**
	 * Enter a parse tree produced by {@link LinterParser#include}.
	 * @param ctx the parse tree
	 */
	void enterInclude(LinterParser.IncludeContext ctx);
	/**
	 * Exit a parse tree produced by {@link LinterParser#include}.
	 * @param ctx the parse tree
	 */
	void exitInclude(LinterParser.IncludeContext ctx);
	/**
	 * Enter a parse tree produced by {@link LinterParser#directive}.
	 * @param ctx the parse tree
	 */
	void enterDirective(LinterParser.DirectiveContext ctx);
	/**
	 * Exit a parse tree produced by {@link LinterParser#directive}.
	 * @param ctx the parse tree
	 */
	void exitDirective(LinterParser.DirectiveContext ctx);
	/**
	 * Enter a parse tree produced by the {@code ImmediateHash}
	 * labeled alternative in {@link LinterParser#immediate}.
	 * @param ctx the parse tree
	 */
	void enterImmediateHash(LinterParser.ImmediateHashContext ctx);
	/**
	 * Exit a parse tree produced by the {@code ImmediateHash}
	 * labeled alternative in {@link LinterParser#immediate}.
	 * @param ctx the parse tree
	 */
	void exitImmediateHash(LinterParser.ImmediateHashContext ctx);
	/**
	 * Enter a parse tree produced by the {@code ImmediateMissingHash}
	 * labeled alternative in {@link LinterParser#immediate}.
	 * @param ctx the parse tree
	 */
	void enterImmediateMissingHash(LinterParser.ImmediateMissingHashContext ctx);
	/**
	 * Exit a parse tree produced by the {@code ImmediateMissingHash}
	 * labeled alternative in {@link LinterParser#immediate}.
	 * @param ctx the parse tree
	 */
	void exitImmediateMissingHash(LinterParser.ImmediateMissingHashContext ctx);
	/**
	 * Enter a parse tree produced by {@link LinterParser#literal}.
	 * @param ctx the parse tree
	 */
	void enterLiteral(LinterParser.LiteralContext ctx);
	/**
	 * Exit a parse tree produced by {@link LinterParser#literal}.
	 * @param ctx the parse tree
	 */
	void exitLiteral(LinterParser.LiteralContext ctx);
	/**
	 * Enter a parse tree produced by {@link LinterParser#op2}.
	 * @param ctx the parse tree
	 */
	void enterOp2(LinterParser.Op2Context ctx);
	/**
	 * Exit a parse tree produced by {@link LinterParser#op2}.
	 * @param ctx the parse tree
	 */
	void exitOp2(LinterParser.Op2Context ctx);
	/**
	 * Enter a parse tree produced by {@link LinterParser#flexop2}.
	 * @param ctx the parse tree
	 */
	void enterFlexop2(LinterParser.Flexop2Context ctx);
	/**
	 * Exit a parse tree produced by {@link LinterParser#flexop2}.
	 * @param ctx the parse tree
	 */
	void exitFlexop2(LinterParser.Flexop2Context ctx);
	/**
	 * Enter a parse tree produced by {@link LinterParser#macroCall}.
	 * @param ctx the parse tree
	 */
	void enterMacroCall(LinterParser.MacroCallContext ctx);
	/**
	 * Exit a parse tree produced by {@link LinterParser#macroCall}.
	 * @param ctx the parse tree
	 */
	void exitMacroCall(LinterParser.MacroCallContext ctx);
	/**
	 * Enter a parse tree produced by {@link LinterParser#conditionalBlock}.
	 * @param ctx the parse tree
	 */
	void enterConditionalBlock(LinterParser.ConditionalBlockContext ctx);
	/**
	 * Exit a parse tree produced by {@link LinterParser#conditionalBlock}.
	 * @param ctx the parse tree
	 */
	void exitConditionalBlock(LinterParser.ConditionalBlockContext ctx);
	/**
	 * Enter a parse tree produced by {@link LinterParser#ifBlock}.
	 * @param ctx the parse tree
	 */
	void enterIfBlock(LinterParser.IfBlockContext ctx);
	/**
	 * Exit a parse tree produced by {@link LinterParser#ifBlock}.
	 * @param ctx the parse tree
	 */
	void exitIfBlock(LinterParser.IfBlockContext ctx);
	/**
	 * Enter a parse tree produced by {@link LinterParser#elseIfBlock}.
	 * @param ctx the parse tree
	 */
	void enterElseIfBlock(LinterParser.ElseIfBlockContext ctx);
	/**
	 * Exit a parse tree produced by {@link LinterParser#elseIfBlock}.
	 * @param ctx the parse tree
	 */
	void exitElseIfBlock(LinterParser.ElseIfBlockContext ctx);
	/**
	 * Enter a parse tree produced by {@link LinterParser#elseBlock}.
	 * @param ctx the parse tree
	 */
	void enterElseBlock(LinterParser.ElseBlockContext ctx);
	/**
	 * Exit a parse tree produced by {@link LinterParser#elseBlock}.
	 * @param ctx the parse tree
	 */
	void exitElseBlock(LinterParser.ElseBlockContext ctx);
	/**
	 * Enter a parse tree produced by {@link LinterParser#ifdefBlock}.
	 * @param ctx the parse tree
	 */
	void enterIfdefBlock(LinterParser.IfdefBlockContext ctx);
	/**
	 * Exit a parse tree produced by {@link LinterParser#ifdefBlock}.
	 * @param ctx the parse tree
	 */
	void exitIfdefBlock(LinterParser.IfdefBlockContext ctx);
	/**
	 * Enter a parse tree produced by {@link LinterParser#ifndefBlock}.
	 * @param ctx the parse tree
	 */
	void enterIfndefBlock(LinterParser.IfndefBlockContext ctx);
	/**
	 * Exit a parse tree produced by {@link LinterParser#ifndefBlock}.
	 * @param ctx the parse tree
	 */
	void exitIfndefBlock(LinterParser.IfndefBlockContext ctx);
	/**
	 * Enter a parse tree produced by the {@code ConstEq}
	 * labeled alternative in {@link LinterParser#constExpr}.
	 * @param ctx the parse tree
	 */
	void enterConstEq(LinterParser.ConstEqContext ctx);
	/**
	 * Exit a parse tree produced by the {@code ConstEq}
	 * labeled alternative in {@link LinterParser#constExpr}.
	 * @param ctx the parse tree
	 */
	void exitConstEq(LinterParser.ConstEqContext ctx);
	/**
	 * Enter a parse tree produced by the {@code ConstMul}
	 * labeled alternative in {@link LinterParser#constExpr}.
	 * @param ctx the parse tree
	 */
	void enterConstMul(LinterParser.ConstMulContext ctx);
	/**
	 * Exit a parse tree produced by the {@code ConstMul}
	 * labeled alternative in {@link LinterParser#constExpr}.
	 * @param ctx the parse tree
	 */
	void exitConstMul(LinterParser.ConstMulContext ctx);
	/**
	 * Enter a parse tree produced by the {@code ConstRel}
	 * labeled alternative in {@link LinterParser#constExpr}.
	 * @param ctx the parse tree
	 */
	void enterConstRel(LinterParser.ConstRelContext ctx);
	/**
	 * Exit a parse tree produced by the {@code ConstRel}
	 * labeled alternative in {@link LinterParser#constExpr}.
	 * @param ctx the parse tree
	 */
	void exitConstRel(LinterParser.ConstRelContext ctx);
	/**
	 * Enter a parse tree produced by the {@code ConstAnd}
	 * labeled alternative in {@link LinterParser#constExpr}.
	 * @param ctx the parse tree
	 */
	void enterConstAnd(LinterParser.ConstAndContext ctx);
	/**
	 * Exit a parse tree produced by the {@code ConstAnd}
	 * labeled alternative in {@link LinterParser#constExpr}.
	 * @param ctx the parse tree
	 */
	void exitConstAnd(LinterParser.ConstAndContext ctx);
	/**
	 * Enter a parse tree produced by the {@code ConstShift}
	 * labeled alternative in {@link LinterParser#constExpr}.
	 * @param ctx the parse tree
	 */
	void enterConstShift(LinterParser.ConstShiftContext ctx);
	/**
	 * Exit a parse tree produced by the {@code ConstShift}
	 * labeled alternative in {@link LinterParser#constExpr}.
	 * @param ctx the parse tree
	 */
	void exitConstShift(LinterParser.ConstShiftContext ctx);
	/**
	 * Enter a parse tree produced by the {@code ConstOr}
	 * labeled alternative in {@link LinterParser#constExpr}.
	 * @param ctx the parse tree
	 */
	void enterConstOr(LinterParser.ConstOrContext ctx);
	/**
	 * Exit a parse tree produced by the {@code ConstOr}
	 * labeled alternative in {@link LinterParser#constExpr}.
	 * @param ctx the parse tree
	 */
	void exitConstOr(LinterParser.ConstOrContext ctx);
	/**
	 * Enter a parse tree produced by the {@code ConstUnary}
	 * labeled alternative in {@link LinterParser#constExpr}.
	 * @param ctx the parse tree
	 */
	void enterConstUnary(LinterParser.ConstUnaryContext ctx);
	/**
	 * Exit a parse tree produced by the {@code ConstUnary}
	 * labeled alternative in {@link LinterParser#constExpr}.
	 * @param ctx the parse tree
	 */
	void exitConstUnary(LinterParser.ConstUnaryContext ctx);
	/**
	 * Enter a parse tree produced by the {@code ConstXor}
	 * labeled alternative in {@link LinterParser#constExpr}.
	 * @param ctx the parse tree
	 */
	void enterConstXor(LinterParser.ConstXorContext ctx);
	/**
	 * Exit a parse tree produced by the {@code ConstXor}
	 * labeled alternative in {@link LinterParser#constExpr}.
	 * @param ctx the parse tree
	 */
	void exitConstXor(LinterParser.ConstXorContext ctx);
	/**
	 * Enter a parse tree produced by the {@code ConstAtom}
	 * labeled alternative in {@link LinterParser#constExpr}.
	 * @param ctx the parse tree
	 */
	void enterConstAtom(LinterParser.ConstAtomContext ctx);
	/**
	 * Exit a parse tree produced by the {@code ConstAtom}
	 * labeled alternative in {@link LinterParser#constExpr}.
	 * @param ctx the parse tree
	 */
	void exitConstAtom(LinterParser.ConstAtomContext ctx);
	/**
	 * Enter a parse tree produced by the {@code ConstAdd}
	 * labeled alternative in {@link LinterParser#constExpr}.
	 * @param ctx the parse tree
	 */
	void enterConstAdd(LinterParser.ConstAddContext ctx);
	/**
	 * Exit a parse tree produced by the {@code ConstAdd}
	 * labeled alternative in {@link LinterParser#constExpr}.
	 * @param ctx the parse tree
	 */
	void exitConstAdd(LinterParser.ConstAddContext ctx);
	/**
	 * Enter a parse tree produced by {@link LinterParser#constPrimary}.
	 * @param ctx the parse tree
	 */
	void enterConstPrimary(LinterParser.ConstPrimaryContext ctx);
	/**
	 * Exit a parse tree produced by {@link LinterParser#constPrimary}.
	 * @param ctx the parse tree
	 */
	void exitConstPrimary(LinterParser.ConstPrimaryContext ctx);
}