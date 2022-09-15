const #funAddr~thr0.base : int;
const #funAddr~thr0.offset : int;
axiom #funAddr~thr0.base == -1 && #funAddr~thr0.offset == 0;
const #funAddr~thr1.base : int;
const #funAddr~thr1.offset : int;
axiom #funAddr~thr1.base == -1 && #funAddr~thr1.offset == 1;
type ~pthread_t~0 = int;
var ~flag0~0 : int;

var ~flag1~0 : int;

var ~turn~0 : int;

var ~x~0 : int;

var ~fairness_label~0 : int;

var #NULL.base : int, #NULL.offset : int;

var #valid : [int]int;

var #length : [int]int;

var #memory_int : [int,int]int;

var #pthreadsForks : int;

var #pthreadsMutex : [int,int]int;

var #StackHeapBarrier : int;

procedure write~int(#value : int, #ptr.base : int, #ptr.offset : int, #sizeOfWrittenType : int) returns ();
ensures #memory_int == old(#memory_int)[#ptr.base,#ptr.offset := #value];
modifies #memory_int;

procedure thr0(#in~_.base : int, #in~_.offset : int) returns (#res.base : int, #res.offset : int);
modifies ~flag0~0, ~turn~0, ~fairness_label~0, ~x~0;

implementation thr0(#in~_.base : int, #in~_.offset : int) returns (#res.base : int, #res.offset : int){
    var #t~post0 : int;
    var ~_.base : int, ~_.offset : int;
    var ~y0~0 : int;

  $Ultimate##0:
    ~_.base, ~_.offset := #in~_.base, #in~_.offset;
    ~flag0~0 := 1;
    ~turn~0 := 1;
    goto $Ultimate##1;
  $Ultimate##1:
    goto $Ultimate##2, $Ultimate##3;
  $Ultimate##2:
    assume true;
    goto Loop~0;
  Loop~0:
    goto $Ultimate##4, $Ultimate##5;
  $Ultimate##4:
    assume !(1 == ~flag1~0 && 1 == ~turn~0);
    goto $Ultimate##6;
  $Ultimate##5:
    assume !!(1 == ~flag1~0 && 1 == ~turn~0);
    ~fairness_label~0 := 0;
    goto $Ultimate##1;
  $Ultimate##3:
    assume !true;
    goto $Ultimate##6;
  $Ultimate##6:
    ~y0~0 := 0;
    ~y0~0 := ~x~0;
    #t~post0 := ~y0~0;
    ~y0~0 := 1 + #t~post0;
    ~x~0 := ~y0~0;
    ~flag0~0 := 0;
    ~fairness_label~0 := 2;
    #res.base, #res.offset := 0, 0;
    return;
}

procedure thr1(#in~_.base : int, #in~_.offset : int) returns (#res.base : int, #res.offset : int);
modifies ~flag1~0, ~turn~0, ~fairness_label~0, ~x~0;

implementation thr1(#in~_.base : int, #in~_.offset : int) returns (#res.base : int, #res.offset : int){
    var #t~post1 : int;
    var ~_.base : int, ~_.offset : int;
    var ~y1~0 : int;

  $Ultimate##0:
    ~_.base, ~_.offset := #in~_.base, #in~_.offset;
    ~flag1~0 := 1;
    ~turn~0 := 0;
    goto $Ultimate##1;
  $Ultimate##1:
    goto $Ultimate##2, $Ultimate##3;
  $Ultimate##2:
    assume true;
    goto Loop~1;
  Loop~1:
    goto $Ultimate##4, $Ultimate##5;
  $Ultimate##4:
    assume !(1 == ~flag0~0 && 0 == ~turn~0);
    goto $Ultimate##6;
  $Ultimate##5:
    assume !!(1 == ~flag0~0 && 0 == ~turn~0);
    ~fairness_label~0 := 1;
    goto $Ultimate##1;
  $Ultimate##3:
    assume !true;
    goto $Ultimate##6;
  $Ultimate##6:
    ~y1~0 := 0;
    ~y1~0 := ~x~0;
    #t~post1 := ~y1~0;
    ~y1~0 := 1 + #t~post1;
    ~x~0 := ~y1~0;
    ~flag1~0 := 0;
    ~fairness_label~0 := 2;
    #res.base, #res.offset := 0, 0;
    return;
}

procedure ULTIMATE.start() returns ();
modifies #NULL.base, #NULL.offset, #valid, ~flag0~0, ~flag1~0, ~turn~0, ~x~0, ~fairness_label~0, #pthreadsForks, #length, #memory_int;

implementation ULTIMATE.start() returns (){
    var #t~ret8 : int;
    var main_#res : int;
    var main_#t~pre2 : int;
    var main_#t~nondet3 : int;
    var main_#t~pre4 : int;
    var main_#t~nondet5 : int;
    var main_#t~mem6 : int;
    var main_#t~mem7 : int;
    var main_~#t0~0.base : int, main_~#t0~0.offset : int;
    var main_~#t1~0.base : int, main_~#t1~0.offset : int;

  $Ultimate##0:
    assume { :begin_inline_ULTIMATE.init } true;
    #NULL.base, #NULL.offset := 0, 0;
    #valid := #valid[0 := 0];
    ~flag0~0 := 0;
    ~flag1~0 := 0;
    ~turn~0 := 0;
    ~x~0 := 0;
    ~fairness_label~0 := 0;
    goto ULTIMATE.init_returnLabel;
  ULTIMATE.init_returnLabel:
    assume { :end_inline_ULTIMATE.init } true;
    assume { :begin_inline_main } true;
    havoc main_#res;
    havoc main_#t~pre2, main_#t~nondet3, main_#t~pre4, main_#t~nondet5, main_#t~mem6, main_#t~mem7, main_~#t0~0.base, main_~#t0~0.offset, main_~#t1~0.base, main_~#t1~0.offset;
    call main_~#t0~0.base, main_~#t0~0.offset := #Ultimate.allocOnStack(8);
    call main_~#t1~0.base, main_~#t1~0.offset := #Ultimate.allocOnStack(8);
	
	
	
    main_#t~pre2 := #pthreadsForks;
    #pthreadsForks := 1 + #pthreadsForks;
    call write~int(main_#t~pre2, main_~#t0~0.base, main_~#t0~0.offset, 8);
    fork main_#t~pre2 thr0(0, 0);
	
	
    main_#t~pre4 := #pthreadsForks;
    #pthreadsForks := 1 + #pthreadsForks;
    call write~int(main_#t~pre4, main_~#t1~0.base, main_~#t1~0.offset, 8);
    fork main_#t~pre4 thr1(0, 0);
	
	
	
    call main_#t~mem6 := read~int(main_~#t0~0.base, main_~#t0~0.offset, 8);
    join main_#t~pre2;
    call main_#t~mem7 := read~int(main_~#t1~0.base, main_~#t1~0.offset, 8);
    join main_#t~pre4;
    main_#res := 0;
    call ULTIMATE.dealloc(main_~#t0~0.base, main_~#t0~0.offset);
    havoc main_~#t0~0.base, main_~#t0~0.offset;
    call ULTIMATE.dealloc(main_~#t1~0.base, main_~#t1~0.offset);
    havoc main_~#t1~0.base, main_~#t1~0.offset;
    goto main_returnLabel;
    call ULTIMATE.dealloc(main_~#t0~0.base, main_~#t0~0.offset);
    havoc main_~#t0~0.base, main_~#t0~0.offset;
    call ULTIMATE.dealloc(main_~#t1~0.base, main_~#t1~0.offset);
    havoc main_~#t1~0.base, main_~#t1~0.offset;
  main_returnLabel:
    #t~ret8 := main_#res;
    assume { :end_inline_main } true;
    return;
}

procedure read~int(#ptr.base : int, #ptr.offset : int, #sizeOfReadType : int) returns (#value : int);
ensures #value == #memory_int[#ptr.base,#ptr.offset];
modifies ;

procedure #Ultimate.allocOnStack(~size : int) returns (#res.base : int, #res.offset : int);
ensures 0 == old(#valid)[#res.base];
ensures #valid == old(#valid)[#res.base := 1];
ensures 0 == #res.offset;
ensures 0 != #res.base;
ensures #StackHeapBarrier < #res.base;
ensures #length == old(#length)[#res.base := ~size];
modifies #valid, #length;

procedure ULTIMATE.dealloc(~addr.base : int, ~addr.offset : int) returns ();
free ensures #valid == old(#valid)[~addr.base := 0];
modifies #valid;

