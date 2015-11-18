import os

standards_for_all = '''#Sat Nov 14 10:48:36 CET 2015
\!/instance/de.uni_freiburg.informatik.ultimate.plugins.generator.blockencoding=
file_export_version=3.0
/instance/de.uni_freiburg.informatik.ultimate.plugins.generator.blockencoding/Rating-Boundary\ (empty\ for\ LBE)=4
@de.uni_freiburg.informatik.ultimate.plugins.generator.blockencoding=0.0.1
/instance/de.uni_freiburg.informatik.ultimate.plugins.generator.blockencoding/Strategy\ for\ the\ edge\ rating=DISJUNCTIVE_RATING
'''

rcfgBuilder_bv = '''#Fri Oct 24 16:34:36 CEST 2014
\!/instance/de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder=
file_export_version=3.0
@de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder=0.0.1
/instance/de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder/Convert\ code\ blocks\ to\ CNF=false
/instance/de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder/Size\ of\ a\ code\ block=SequenceOfStatements
/instance/de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder/Command\ for\ external\ solver=~/z3/z3 SMTLIB2_COMPLIANT\=true -memory\:2024 -smt2 -in -t\:2000
/instance/de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder/Logic\ for\ external\ solver=AUFBV
/instance/de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder/Dump\ SMT\ script\ to\ file=false
/instance/de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder/To\ the\ following\ directory=./dump/
 '''

rcfgBuilder_nonbv = '''#Fri Oct 24 16:34:36 CEST 2014
\!/instance/de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder=
file_export_version=3.0
@de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder=0.0.1
/instance/de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder/Convert\ code\ blocks\ to\ CNF=false
/instance/de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder/Size\ of\ a\ code\ block=SequenceOfStatements
/instance/de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder/Command\ for\ external\ solver=~/z3/z3 SMTLIB2_COMPLIANT\=true -memory\:2024 -smt2 -in -t\:2000
/instance/de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder/Dump\ SMT\ script\ to\ file=false
/instance/de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder/To\ the\ following\ directory=./dump/
 '''

cacsl_memsafety_nonbv = '''#Fri Oct 24 16:34:36 CEST 2014
\!/instance/de.uni_freiburg.informatik.ultimate.plugins.generator.cacsl2boogietranslator=
/instance/de.uni_freiburg.informatik.ultimate.plugins.generator.cacsl2boogietranslator/Translation\ Mode\:=SV_COMP14
/instance/de.uni_freiburg.informatik.ultimate.plugins.generator.cacsl2boogietranslator/Checked\ method.\ Library\ mode\ if\ empty.=main
/instance/de.uni_freiburg.informatik.ultimate.plugins.generator.cacsl2boogietranslator/sizeof\ long=4
/instance/de.uni_freiburg.informatik.ultimate.plugins.generator.cacsl2boogietranslator/sizeof\ POINTER=4
/instance/de.uni_freiburg.informatik.ultimate.plugins.generator.cacsl2boogietranslator/sizeof\ long\ double=12
/instance/de.uni_freiburg.informatik.ultimate.plugins.generator.cacsl2boogietranslator/Check\ division\ by\ zero=IGNORE
/instance/de.uni_freiburg.informatik.ultimate.plugins.generator.cacsl2boogietranslator/Check\ for\ the\ main\ procedure\ if\ all\ allocated\ memory\ was\ freed=true
/instance/de.uni_freiburg.informatik.ultimate.plugins.generator.cacsl2boogietranslator/If\ two\ pointers\ are\ subtracted\ or\ compared\ they\ have\ the\ same\ base\ address=IGNORE
@de.uni_freiburg.informatik.ultimate.plugins.generator.cacsl2boogietranslator=0.0.1
 '''

cacsl_reach_bv = '''#Fri Oct 24 16:34:36 CEST 2014
\!/instance/de.uni_freiburg.informatik.ultimate.plugins.generator.cacsl2boogietranslator=
/instance/de.uni_freiburg.informatik.ultimate.plugins.generator.cacsl2boogietranslator/Translation\ Mode\:=SV_COMP14
/instance/de.uni_freiburg.informatik.ultimate.plugins.generator.cacsl2boogietranslator/Checked\ method.\ Library\ mode\ if\ empty.=main
/instance/de.uni_freiburg.informatik.ultimate.plugins.generator.cacsl2boogietranslator/sizeof\ long=4
/instance/de.uni_freiburg.informatik.ultimate.plugins.generator.cacsl2boogietranslator/sizeof\ POINTER=4
/instance/de.uni_freiburg.informatik.ultimate.plugins.generator.cacsl2boogietranslator/sizeof\ long\ double=12
/instance/de.uni_freiburg.informatik.ultimate.plugins.generator.cacsl2boogietranslator/Check\ division\ by\ zero=IGNORE
/instance/de.uni_freiburg.informatik.ultimate.plugins.generator.cacsl2boogietranslator/Check\ if\ freed\ pointer\ was\ valid=false
/instance/de.uni_freiburg.informatik.ultimate.plugins.generator.cacsl2boogietranslator/Pointer\ to\ allocated\ memory\ at\ dereference=IGNORE
/instance/de.uni_freiburg.informatik.ultimate.plugins.generator.cacsl2boogietranslator/Check\ array\ bounds\ for\ arrays\ that\ are\ off\ heap=IGNORE
/instance/de.uni_freiburg.informatik.ultimate.plugins.generator.cacsl2boogietranslator/Check\ for\ the\ main\ procedure\ if\ all\ allocated\ memory\ was\ freed=false
/instance/de.uni_freiburg.informatik.ultimate.plugins.generator.cacsl2boogietranslator/If\ two\ pointers\ are\ subtracted\ or\ compared\ they\ have\ the\ same\ base\ address=IGNORE
/instance/de.uni_freiburg.informatik.ultimate.plugins.generator.cacsl2boogietranslator/Pointer\ base\ address\ is\ valid\ at\ dereference=IGNORE
/instance/de.uni_freiburg.informatik.ultimate.plugins.generator.cacsl2boogietranslator/Use\ bitvectors\ instead\ of\ ints=true
@de.uni_freiburg.informatik.ultimate.plugins.generator.cacsl2boogietranslator=0.0.1
 '''

cacsl_reach_nonbv = '''#Fri Oct 24 16:34:36 CEST 2014
\!/instance/de.uni_freiburg.informatik.ultimate.plugins.generator.cacsl2boogietranslator=
/instance/de.uni_freiburg.informatik.ultimate.plugins.generator.cacsl2boogietranslator/Translation\ Mode\:=SV_COMP14
/instance/de.uni_freiburg.informatik.ultimate.plugins.generator.cacsl2boogietranslator/Checked\ method.\ Library\ mode\ if\ empty.=main
/instance/de.uni_freiburg.informatik.ultimate.plugins.generator.cacsl2boogietranslator/sizeof\ long=4
/instance/de.uni_freiburg.informatik.ultimate.plugins.generator.cacsl2boogietranslator/sizeof\ POINTER=4
/instance/de.uni_freiburg.informatik.ultimate.plugins.generator.cacsl2boogietranslator/sizeof\ long\ double=12
/instance/de.uni_freiburg.informatik.ultimate.plugins.generator.cacsl2boogietranslator/Check\ division\ by\ zero=IGNORE
/instance/de.uni_freiburg.informatik.ultimate.plugins.generator.cacsl2boogietranslator/Check\ if\ freed\ pointer\ was\ valid=false
/instance/de.uni_freiburg.informatik.ultimate.plugins.generator.cacsl2boogietranslator/Pointer\ to\ allocated\ memory\ at\ dereference=IGNORE
/instance/de.uni_freiburg.informatik.ultimate.plugins.generator.cacsl2boogietranslator/Check\ array\ bounds\ for\ arrays\ that\ are\ off\ heap=IGNORE
/instance/de.uni_freiburg.informatik.ultimate.plugins.generator.cacsl2boogietranslator/Check\ for\ the\ main\ procedure\ if\ all\ allocated\ memory\ was\ freed=false
/instance/de.uni_freiburg.informatik.ultimate.plugins.generator.cacsl2boogietranslator/If\ two\ pointers\ are\ subtracted\ or\ compared\ they\ have\ the\ same\ base\ address=IGNORE
/instance/de.uni_freiburg.informatik.ultimate.plugins.generator.cacsl2boogietranslator/Pointer\ base\ address\ is\ valid\ at\ dereference=IGNORE
@de.uni_freiburg.informatik.ultimate.plugins.generator.cacsl2boogietranslator=0.0.1
 '''

dateline = '''#Wed Nov 18 19:26:57 CET 2015'''
codecheckCommon = '''@de.uni_freiburg.informatik.ultimate.plugins.generator.codecheck=0.0.1
/instance/de.uni_freiburg.informatik.ultimate.plugins.generator.codecheck/Use\ standard\ solver\ (from\ RCFGBuilder)\ with\ FP\ interpolation\ as\ fallback=false
file_export_version=3.0'''

treeItp = '''/instance/de.uni_freiburg.informatik.ultimate.plugins.generator.codecheck/interpolation\ mode=Craig_TreeInterpolation'''
nestedItp = '''/instance/de.uni_freiburg.informatik.ultimate.plugins.generator.codecheck/interpolation\ mode=Craig_NestedInterpolation'''
fpItp = '''/instance/de.uni_freiburg.informatik.ultimate.plugins.generator.codecheck/interpolation\ mode=ForwardPredicates'''
bpItp = '''/instance/de.uni_freiburg.informatik.ultimate.plugins.generator.codecheck/interpolation\ mode=BackwardPredicates'''

chooseExternalDefault = '''/instance/de.uni_freiburg.informatik.ultimate.plugins.generator.codecheck/Choose\ which\ separate\ solver\ to\ use\ for\ tracechecks=External_DefaultMode
/instance/de.uni_freiburg.informatik.ultimate.plugins.generator.codecheck/Theory\ for\ external\ solver=AUFNIRA/instance/de.uni_freiburg.informatik.ultimate.plugins.generator.codecheck/Command\ for\ calling\ external\ solver=~/z3/z3 SMTLIB2_COMPLIANT\=true -memory\:2024 -smt2 -in -t\:12000'''

chooseIZ3 = '''/instance/de.uni_freiburg.informatik.ultimate.plugins.generator.codecheck/interpolation\ mode=Craig_NestedInterpolation
/instance/de.uni_freiburg.informatik.ultimate.plugins.generator.codecheck/Choose\ which\ separate\ solver\ to\ use\ for\ tracechecks=External_Z3InterpolationMode
/instance/de.uni_freiburg.informatik.ultimate.plugins.generator.codecheck/Theory\ for\ external\ solver=AUFNIRA/instance/de.uni_freiburg.informatik.ultimate.plugins.generator.codecheck/Command\ for\ calling\ external\ solver=~/z3/z3 SMTLIB2_COMPLIANT\=true -memory\:2024 -smt2 -in -t\:12000'''

chooseSMTInterpol = '''/instance/de.uni_freiburg.informatik.ultimate.plugins.generator.codecheck/interpolation\ mode=Craig_TreeInterpolation
/instance/de.uni_freiburg.informatik.ultimate.plugins.generator.codecheck/Choose\ which\ separate\ solver\ to\ use\ for\ tracechecks=External_SMTInterpolInterpolationMode
/instance/de.uni_freiburg.informatik.ultimate.plugins.generator.codecheck/Theory\ for\ external\ solver=QF_AUFLIRA
/instance/de.uni_freiburg.informatik.ultimate.plugins.generator.codecheck/Command\ for\ calling\ external\ solver=java -jar ~/smtinterpol/smtinterpol.jar -q -t 12000'''

choosePrincess = '''/instance/de.uni_freiburg.informatik.ultimate.plugins.generator.codecheck/interpolation\ mode=Craig_TreeInterpolation
/instance/de.uni_freiburg.informatik.ultimate.plugins.generator.codecheck/Choose\ which\ separate\ solver\ to\ use\ for\ tracechecks=External_PrincessInterpolationMode
/instance/de.uni_freiburg.informatik.ultimate.plugins.generator.codecheck/Theory\ for\ external\ solver=AUFNIRA
/instance/de.uni_freiburg.informatik.ultimate.plugins.generator.codecheck/Command\ for\ calling\ external\ solver=~/princess/princess +incremental +stdin -timeout=12000'''

dontUseLV = '''/instance/de.uni_freiburg.informatik.ultimate.plugins.generator.codecheck//Use\ live\ variables\ in\ FP/BP\ interpolation=false'''
useLV = '''/instance/de.uni_freiburg.informatik.ultimate.plugins.generator.codecheck//Use\ live\ variables\ in\ FP/BP\ interpolation=true'''

useUC = ''''''
dontUseUC = '''/instance/de.uni_freiburg.informatik.ultimate.plugins.generator.codecheck//Use\ unsat\ cores\ in\ FP/BP\ interpolation=IGNORE'''

for a, b, c in os.walk("."):
 for fn in c:
  if fn[-4:] == '.epf':
   print(fn)
   f = open(fn, 'w')

   #common -- BlockEncoding
   print(standards_for_all, file=f)
   #print("", file=f)

   #RCFGBuilder
   if 'Bitvector' in fn:
    print(rcfgBuilder_bv, file=f)
    #print("", file=f)
   elif 'Integer' in fn:
    print(rcfgBuilder_nonbv, file=f)
    #print("", file=f)
   else:
    #print(rcfgBuilder_nonbv, file=f) #default: Integer
    #print("", file=f)
    print('ERROR: neither Integer nor Bitvector in filename')

   #C translation 

   if 'Reach' in fn and 'Bitvector' in fn:
     print(cacsl_reach_bv, file=f)
   elif 'Reach' in fn and 'Integer' in fn:
     print(cacsl_reach_nonbv, file=f)
   elif 'DerefFreeMemtrack' in fn and 'Integer' in fn:
     print(cacsl_memsafety_nonbv, file=f)
   #elif 'Reach' in fn:
     #if neither Bitvector nor Integer occurs, use Integer
     #print(cacsl_reach_nonbv, file=f)
   else:
    print('ERROR: did not recognize translation mode')

   #codecheck interpolation settings
   
   print(dateline, file=f)
   print(codecheckCommon, file=f)

   if 'TreeInterpolation' in fn:
    print(treeItp, file=f)
   elif 'NestedInterpolation' in fn:
    print(nestedItp, file=f)
   elif 'FP' in fn:
    print(fpItp, file=f)
   elif 'BP' in fn:
    print(bpItp, file=f)
   else:
    print('ERROR: did not recognize interpolation mode')

   if 'SMTInterpol' in fn:
    print(chooseSMTInterpol, file=f)
   elif 'iZ3' in fn:
    print(chooseIZ3, file=f)
   elif '-Z3-' in fn:
    print(chooseExternalDefault, file=f)
   elif 'Princess' in fn:
    print(choosePrincess, file=f)
   else:
    print('ERROR: did not recognize solver to use')

   if 'LV' not in fn:
    print(dontUseLV, file=f)
   elif 'LV' in fn:
    print(useLV, file=f)

   if 'UC' not in fn:
    print(dontUseUC, file=f)
   elif 'UC' in fn:
    print(useUC, file=f)

    
    


