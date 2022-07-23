const #funAddr~thr1.base : int;
const #funAddr~thr1.offset : int;
axiom #funAddr~thr1.base == -1 && #funAddr~thr1.offset == 0;
const #funAddr~thr2.base : int;
const #funAddr~thr2.offset : int;
axiom #funAddr~thr2.base == -1 && #funAddr~thr2.offset == 1;
type ~pthread_t~0 = int;
var ~#flag~0.base : int, ~#flag~0.offset : int;

var ~turn~0 : int;

var ~x~0 : int;

var #NULL.base : int, #NULL.offset : int;

var #valid : [int]int;

var #length : [int]int;

var #memory_int : [int,int]int;

var #pthreadsForks : int;

var #StackHeapBarrier : int;

procedure write~int(#value : int, #ptr.base : int, #ptr.offset : int, #sizeOfWrittenType : int) returns ();
ensures #memory_int == old(#memory_int)[#ptr.base,#ptr.offset := #value];
modifies #memory_int;

procedure thr2(#in~_.base : int, #in~_.offset : int) returns (#res.base : int, #res.offset : int);
modifies ~turn~0, ~x~0, #memory_int;

implementation thr2(#in~_.base : int, #in~_.offset : int) returns (#res.base : int, #res.offset : int){
    var #t~mem3 : int;
    var #t~mem4 : int;
    var #t~post5 : int;
    var ~_.base : int, ~_.offset : int;
    var ~f12~0 : int;
    var ~t2~0 : int;

  $Ultimate##0:
    ~_.base, ~_.offset := #in~_.base, #in~_.offset;
    call write~int(1, ~#flag~0.base, 4 + ~#flag~0.offset, 4);
    ~turn~0 := 0;
    call #t~mem3 := read~int(~#flag~0.base, ~#flag~0.offset, 4);
    ~f12~0 := #t~mem3;
    havoc #t~mem3;
    ~t2~0 := ~turn~0;
    goto $Ultimate##1;
  $Ultimate##1:
    goto $Ultimate##2, $Ultimate##3;
  $Ultimate##2:
    assume true;
    goto Loop~1;
  Loop~1:
    goto $Ultimate##4, $Ultimate##5;
  $Ultimate##4:
    assume !(1 == ~f12~0 && 0 == ~t2~0);
    goto $Ultimate##6;
  $Ultimate##5:
    assume !!(1 == ~f12~0 && 0 == ~t2~0);
    call #t~mem4 := read~int(~#flag~0.base, ~#flag~0.offset, 4);
    ~f12~0 := #t~mem4;
    ~t2~0 := ~turn~0;
    goto $Ultimate##1;
  $Ultimate##3:
    assume !true;
    goto $Ultimate##6;
  $Ultimate##6:
    #t~post5 := ~x~0;
    ~x~0 := 1 + #t~post5;
    call write~int(0, ~#flag~0.base, 4 + ~#flag~0.offset, 4);
    #res.base, #res.offset := 0, 0;
    return;
}

procedure thr1(#in~_.base : int, #in~_.offset : int) returns (#res.base : int, #res.offset : int);
modifies ~turn~0, ~x~0, #memory_int;

implementation thr1(#in~_.base : int, #in~_.offset : int) returns (#res.base : int, #res.offset : int){
    var #t~mem0 : int;
    var #t~mem1 : int;
    var #t~post2 : int;
    var ~_.base : int, ~_.offset : int;
    var ~f21~0 : int;
    var ~t1~0 : int;

  $Ultimate##0:
    ~_.base, ~_.offset := #in~_.base, #in~_.offset;
    call write~int(1, ~#flag~0.base, ~#flag~0.offset, 4);
    ~turn~0 := 1;
    call #t~mem0 := read~int(~#flag~0.base, 4 + ~#flag~0.offset, 4);
    ~f21~0 := #t~mem0;
    havoc #t~mem0;
    ~t1~0 := ~turn~0;
    goto $Ultimate##1;
  $Ultimate##1:
    goto $Ultimate##2, $Ultimate##3;
  $Ultimate##2:
    assume true;
    goto Loop~0;
  Loop~0:
    goto $Ultimate##4, $Ultimate##5;
  $Ultimate##4:
    assume !(1 == ~f21~0 && 1 == ~t1~0);
    goto $Ultimate##6;
  $Ultimate##5:
    assume !!(1 == ~f21~0 && 1 == ~t1~0);
    call #t~mem1 := read~int(~#flag~0.base, 4 + ~#flag~0.offset, 4);
    ~f21~0 := #t~mem1;
    ~t1~0 := ~turn~0;
    goto $Ultimate##1;
  $Ultimate##3:
    assume !true;
    goto $Ultimate##6;
  $Ultimate##6:
    #t~post2 := ~x~0;
    ~x~0 := 1 + #t~post2;
    call write~int(0, ~#flag~0.base, ~#flag~0.offset, 4);
    #res.base, #res.offset := 0, 0;
    return;
}

procedure ULTIMATE.start() returns ();
modifies #pthreadsForks, #valid, #memory_int, #length, #NULL.base, #NULL.offset, ~#flag~0.base, ~#flag~0.offset, ~turn~0, ~x~0;

implementation ULTIMATE.start() returns (){
    var #t~ret12 : int;
    var main_#res : int;
    var main_#t~pre6 : int;
    var main_#t~nondet7 : int;
    var main_#t~pre8 : int;
    var main_#t~nondet9 : int;
    var main_#t~mem10 : int;
    var main_#t~mem11 : int;
    var main_~#t1~1.base : int, main_~#t1~1.offset : int;
    var main_~#t2~1.base : int, main_~#t2~1.offset : int;

  $Ultimate##0:
    assume { :begin_inline_ULTIMATE.init } true;
    #NULL.base, #NULL.offset := 0, 0;
    #valid := #valid[0 := 0];
    call ~#flag~0.base, ~#flag~0.offset := #Ultimate.allocOnStack(8);
    call write~init~int(0, ~#flag~0.base, ~#flag~0.offset, 4);
    call write~init~int(0, ~#flag~0.base, 4 + ~#flag~0.offset, 4);
    ~turn~0 := 0;
    ~x~0 := 0;
    goto ULTIMATE.init_returnLabel;
  ULTIMATE.init_returnLabel:
    assume { :end_inline_ULTIMATE.init } true;
    assume { :begin_inline_main } true;
    havoc main_#res;
    havoc main_#t~pre6, main_#t~nondet7, main_#t~pre8, main_#t~nondet9, main_#t~mem10, main_#t~mem11, main_~#t1~1.base, main_~#t1~1.offset, main_~#t2~1.base, main_~#t2~1.offset;
    call main_~#t1~1.base, main_~#t1~1.offset := #Ultimate.allocOnStack(8);
    call main_~#t2~1.base, main_~#t2~1.offset := #Ultimate.allocOnStack(8);
    main_#t~pre6 := #pthreadsForks;
    #pthreadsForks := 1 + #pthreadsForks;
    call write~int(main_#t~pre6, main_~#t1~1.base, main_~#t1~1.offset, 8);
    fork main_#t~pre6 thr1(0, 0);
    main_#t~pre8 := #pthreadsForks;
    #pthreadsForks := 1 + #pthreadsForks;
    call write~int(main_#t~pre8, main_~#t2~1.base, main_~#t2~1.offset, 8);
    fork main_#t~pre8 thr2(0, 0);
    call main_#t~mem10 := read~int(main_~#t1~1.base, main_~#t1~1.offset, 8);
    join main_#t~pre6;
    call main_#t~mem11 := read~int(main_~#t2~1.base, main_~#t2~1.offset, 8);
    join main_#t~pre8;
    main_#res := 0;
    call ULTIMATE.dealloc(main_~#t1~1.base, main_~#t1~1.offset);
    havoc main_~#t1~1.base, main_~#t1~1.offset;
    call ULTIMATE.dealloc(main_~#t2~1.base, main_~#t2~1.offset);
    havoc main_~#t2~1.base, main_~#t2~1.offset;
    goto main_returnLabel;
    call ULTIMATE.dealloc(main_~#t1~1.base, main_~#t1~1.offset);
    havoc main_~#t1~1.base, main_~#t1~1.offset;
    call ULTIMATE.dealloc(main_~#t2~1.base, main_~#t2~1.offset);
    havoc main_~#t2~1.base, main_~#t2~1.offset;
  main_returnLabel:
    #t~ret12 := main_#res;
    assume { :end_inline_main } true;
    return;
}

procedure read~int(#ptr.base : int, #ptr.offset : int, #sizeOfReadType : int) returns (#value : int);
ensures #value == #memory_int[#ptr.base,#ptr.offset];
modifies ;

procedure write~init~int(#value : int, #ptr.base : int, #ptr.offset : int, #sizeOfWrittenType : int) returns ();
ensures #memory_int[#ptr.base,#ptr.offset] == #value;
modifies #memory_int;

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

