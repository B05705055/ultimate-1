const #funAddr~thr1.base : int;
const #funAddr~thr1.offset : int;
axiom #funAddr~thr1.base == -1 && #funAddr~thr1.offset == 0;
const #funAddr~thr2.base : int;
const #funAddr~thr2.offset : int;
axiom #funAddr~thr2.base == -1 && #funAddr~thr2.offset == 1;
type ~pthread_t~0 = int;
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
modifies ~x~0;

implementation thr2(#in~_.base : int, #in~_.offset : int) returns (#res.base : int, #res.offset : int){
    var ~_.base : int, ~_.offset : int;

  $Ultimate##0:
    ~_.base, ~_.offset := #in~_.base, #in~_.offset;
    ~x~0 := 2;
    return;
}

procedure thr1(#in~_.base : int, #in~_.offset : int) returns (#res.base : int, #res.offset : int);
modifies ~x~0;

implementation thr1(#in~_.base : int, #in~_.offset : int) returns (#res.base : int, #res.offset : int){
    var ~_.base : int, ~_.offset : int;

  $Ultimate##0:
    ~_.base, ~_.offset := #in~_.base, #in~_.offset;
    ~x~0 := 1;
    return;
}

procedure ULTIMATE.start() returns ();
modifies #NULL.base, #NULL.offset, #valid, ~x~0, #pthreadsForks, #memory_int, #length;

implementation ULTIMATE.start() returns (){
    var #t~ret6 : int;
    var main_#res : int;
    var main_#t~pre0 : int;
    var main_#t~nondet1 : int;
    var main_#t~pre2 : int;
    var main_#t~nondet3 : int;
    var main_#t~mem4 : int;
    var main_#t~mem5 : int;
    var main_~#t1~0.base : int, main_~#t1~0.offset : int;
    var main_~#t2~0.base : int, main_~#t2~0.offset : int;

  $Ultimate##0:
    assume { :begin_inline_ULTIMATE.init } true;
    #NULL.base, #NULL.offset := 0, 0;
    #valid := #valid[0 := 0];
    ~x~0 := 0;
    goto ULTIMATE.init_returnLabel;
  ULTIMATE.init_returnLabel:
    assume { :end_inline_ULTIMATE.init } true;
    assume { :begin_inline_main } true;
    havoc main_#res;
    havoc main_#t~pre0, main_#t~nondet1, main_#t~pre2, main_#t~nondet3, main_#t~mem4, main_#t~mem5, main_~#t1~0.base, main_~#t1~0.offset, main_~#t2~0.base, main_~#t2~0.offset;
    call main_~#t1~0.base, main_~#t1~0.offset := #Ultimate.allocOnStack(8);
    call main_~#t2~0.base, main_~#t2~0.offset := #Ultimate.allocOnStack(8);
    main_#t~pre0 := #pthreadsForks;
    #pthreadsForks := 1 + #pthreadsForks;
    call write~int(main_#t~pre0, main_~#t1~0.base, main_~#t1~0.offset, 8);
    fork main_#t~pre0 thr1(0, 0);
    main_#t~pre2 := #pthreadsForks;
    #pthreadsForks := 1 + #pthreadsForks;
    call write~int(main_#t~pre2, main_~#t2~0.base, main_~#t2~0.offset, 8);
    fork main_#t~pre2 thr2(0, 0);
    call main_#t~mem4 := read~int(main_~#t1~0.base, main_~#t1~0.offset, 8);
    join main_#t~pre0;
    call main_#t~mem5 := read~int(main_~#t2~0.base, main_~#t2~0.offset, 8);
    join main_#t~pre2;
    main_#res := 0;
    call ULTIMATE.dealloc(main_~#t1~0.base, main_~#t1~0.offset);
    havoc main_~#t1~0.base, main_~#t1~0.offset;
    call ULTIMATE.dealloc(main_~#t2~0.base, main_~#t2~0.offset);
    havoc main_~#t2~0.base, main_~#t2~0.offset;
    goto main_returnLabel;
    call ULTIMATE.dealloc(main_~#t1~0.base, main_~#t1~0.offset);
    havoc main_~#t1~0.base, main_~#t1~0.offset;
    call ULTIMATE.dealloc(main_~#t2~0.base, main_~#t2~0.offset);
    havoc main_~#t2~0.base, main_~#t2~0.offset;
  main_returnLabel:
    #t~ret6 := main_#res;
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

