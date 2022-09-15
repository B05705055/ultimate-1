const #funAddr~thr.base : int;
const #funAddr~thr.offset : int;
axiom #funAddr~thr.base == -1 && #funAddr~thr.offset == 0;
type ~pthread_t~0 = int;
var ~x~0 : int;

var ~#level~0.base : int, ~#level~0.offset : int;

var ~#last_to_enter~0.base : int, ~#last_to_enter~0.offset : int;

var ~#thr_num_array~0.base : int, ~#thr_num_array~0.offset : int;

var ~fairness_label_num~0 : int;

var ~fairness_label_verified~0 : int;

var ~fairness_label_end~0 : int;

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

procedure ULTIMATE.start() returns ();
modifies #pthreadsForks, #memory_int, #valid, #length, ~fairness_label_num~0, ~fairness_label_verified~0, ~x~0, ~fairness_label_end~0, #NULL.base, #NULL.offset, ~#level~0.base, ~#level~0.offset, ~#last_to_enter~0.base, ~#last_to_enter~0.offset, ~#thr_num_array~0.base, ~#thr_num_array~0.offset;

implementation ULTIMATE.start() returns (){
    var #t~ret22 : int;
    var main_#res : int;
    var main_#t~post9 : int;
    var main_~i~1 : int;
    var main_#t~pre10 : int;
    var main_#t~nondet11 : int;
    var main_#t~pre12 : int;
    var main_#t~nondet13 : int;
    var main_#t~pre14 : int;
    var main_#t~nondet15 : int;
    var main_#t~pre16 : int;
    var main_#t~nondet17 : int;
    var main_#t~mem18 : int;
    var main_#t~mem19 : int;
    var main_#t~mem20 : int;
    var main_#t~mem21 : int;
    var main_~#t0~0.base : int, main_~#t0~0.offset : int;
    var main_~#t1~0.base : int, main_~#t1~0.offset : int;
    var main_~#t2~0.base : int, main_~#t2~0.offset : int;
    var main_~#t3~0.base : int, main_~#t3~0.offset : int;

  $Ultimate##0:
    assume { :begin_inline_ULTIMATE.init } true;
    #NULL.base, #NULL.offset := 0, 0;
    #valid := #valid[0 := 0];
    ~x~0 := 0;
    call ~#level~0.base, ~#level~0.offset := #Ultimate.allocOnStack(16);
    call write~init~int(-1, ~#level~0.base, ~#level~0.offset, 4);
    call write~init~int(0, ~#level~0.base, 4 + ~#level~0.offset, 4);
    call write~init~int(0, ~#level~0.base, 8 + ~#level~0.offset, 4);
    call write~init~int(0, ~#level~0.base, 12 + ~#level~0.offset, 4);
    call ~#last_to_enter~0.base, ~#last_to_enter~0.offset := #Ultimate.allocOnStack(12);
    call write~init~int(-1, ~#last_to_enter~0.base, ~#last_to_enter~0.offset, 4);
    call write~init~int(0, ~#last_to_enter~0.base, 4 + ~#last_to_enter~0.offset, 4);
    call write~init~int(0, ~#last_to_enter~0.base, 8 + ~#last_to_enter~0.offset, 4);
    call ~#thr_num_array~0.base, ~#thr_num_array~0.offset := #Ultimate.allocOnStack(16);
    call write~init~int(0, ~#thr_num_array~0.base, ~#thr_num_array~0.offset, 4);
    call write~init~int(0, ~#thr_num_array~0.base, 4 + ~#thr_num_array~0.offset, 4);
    call write~init~int(0, ~#thr_num_array~0.base, 8 + ~#thr_num_array~0.offset, 4);
    call write~init~int(0, ~#thr_num_array~0.base, 12 + ~#thr_num_array~0.offset, 4);
    ~fairness_label_num~0 := 0;
    ~fairness_label_verified~0 := 0;
    ~fairness_label_end~0 := 0;
    goto ULTIMATE.init_returnLabel;
  ULTIMATE.init_returnLabel:
    assume { :end_inline_ULTIMATE.init } true;
    assume { :begin_inline_main } true;
    havoc main_#res;
    havoc main_#t~post9, main_~i~1, main_#t~pre10, main_#t~nondet11, main_#t~pre12, main_#t~nondet13, main_#t~pre14, main_#t~nondet15, main_#t~pre16, main_#t~nondet17, main_#t~mem18, main_#t~mem19, main_#t~mem20, main_#t~mem21, main_~#t0~0.base, main_~#t0~0.offset, main_~#t1~0.base, main_~#t1~0.offset, main_~#t2~0.base, main_~#t2~0.offset, main_~#t3~0.base, main_~#t3~0.offset;
    main_~i~1 := 0;
    goto $Ultimate##1;
  $Ultimate##1:
    goto $Ultimate##2, $Ultimate##3;
  $Ultimate##2:
    assume true;
    goto $Ultimate##4, $Ultimate##5;
  $Ultimate##4:
    assume !(main_~i~1 < 4);
    goto $Ultimate##6;
  $Ultimate##5:
    assume !!(main_~i~1 < 4);
    call write~int(main_~i~1, ~#thr_num_array~0.base, ~#thr_num_array~0.offset + 4 * main_~i~1, 4);
    goto main_Loop~3;
  main_Loop~3:
    main_#t~post9 := main_~i~1;
    main_~i~1 := 1 + main_#t~post9;
    havoc main_#t~post9;
    goto $Ultimate##1;
  $Ultimate##3:
    assume !true;
    goto $Ultimate##6;
  $Ultimate##6:
    call main_~#t0~0.base, main_~#t0~0.offset := #Ultimate.allocOnStack(8);
    call main_~#t1~0.base, main_~#t1~0.offset := #Ultimate.allocOnStack(8);
    call main_~#t2~0.base, main_~#t2~0.offset := #Ultimate.allocOnStack(8);
    call main_~#t3~0.base, main_~#t3~0.offset := #Ultimate.allocOnStack(8);
    main_#t~pre10 := #pthreadsForks;
    #pthreadsForks := 1 + #pthreadsForks;
    call write~int(main_#t~pre10, main_~#t0~0.base, main_~#t0~0.offset, 8);
    fork main_#t~pre10 thr(~#thr_num_array~0.base, ~#thr_num_array~0.offset);
    main_#t~pre12 := #pthreadsForks;
    #pthreadsForks := 1 + #pthreadsForks;
    call write~int(main_#t~pre12, main_~#t1~0.base, main_~#t1~0.offset, 8);
    fork main_#t~pre12 thr(~#thr_num_array~0.base, 4 + ~#thr_num_array~0.offset);
    main_#t~pre14 := #pthreadsForks;
    #pthreadsForks := 1 + #pthreadsForks;
    call write~int(main_#t~pre14, main_~#t2~0.base, main_~#t2~0.offset, 8);
    fork main_#t~pre14 thr(~#thr_num_array~0.base, 8 + ~#thr_num_array~0.offset);
    main_#t~pre16 := #pthreadsForks;
    #pthreadsForks := 1 + #pthreadsForks;
    call write~int(main_#t~pre16, main_~#t3~0.base, main_~#t3~0.offset, 8);
    fork main_#t~pre16 thr(~#thr_num_array~0.base, 12 + ~#thr_num_array~0.offset);
    call main_#t~mem18 := read~int(main_~#t0~0.base, main_~#t0~0.offset, 8);
    join main_#t~pre10;
    call main_#t~mem19 := read~int(main_~#t1~0.base, main_~#t1~0.offset, 8);
    join main_#t~pre12;
    call main_#t~mem20 := read~int(main_~#t2~0.base, main_~#t2~0.offset, 8);
    join main_#t~pre14;
    call main_#t~mem21 := read~int(main_~#t3~0.base, main_~#t3~0.offset, 8);
    join main_#t~pre16;
    main_#res := 0;
    call ULTIMATE.dealloc(main_~#t0~0.base, main_~#t0~0.offset);
    havoc main_~#t0~0.base, main_~#t0~0.offset;
    call ULTIMATE.dealloc(main_~#t1~0.base, main_~#t1~0.offset);
    havoc main_~#t1~0.base, main_~#t1~0.offset;
    call ULTIMATE.dealloc(main_~#t2~0.base, main_~#t2~0.offset);
    havoc main_~#t2~0.base, main_~#t2~0.offset;
    call ULTIMATE.dealloc(main_~#t3~0.base, main_~#t3~0.offset);
    havoc main_~#t3~0.base, main_~#t3~0.offset;
    goto main_returnLabel;
    call ULTIMATE.dealloc(main_~#t0~0.base, main_~#t0~0.offset);
    havoc main_~#t0~0.base, main_~#t0~0.offset;
    call ULTIMATE.dealloc(main_~#t1~0.base, main_~#t1~0.offset);
    havoc main_~#t1~0.base, main_~#t1~0.offset;
    call ULTIMATE.dealloc(main_~#t2~0.base, main_~#t2~0.offset);
    havoc main_~#t2~0.base, main_~#t2~0.offset;
    call ULTIMATE.dealloc(main_~#t3~0.base, main_~#t3~0.offset);
    havoc main_~#t3~0.base, main_~#t3~0.offset;
  main_returnLabel:
    #t~ret22 := main_#res;
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

procedure thr(#in~k.base : int, #in~k.offset : int) returns (#res.base : int, #res.offset : int);
modifies ~fairness_label_num~0, ~fairness_label_verified~0, ~x~0, ~fairness_label_end~0, #memory_int;

implementation thr(#in~k.base : int, #in~k.offset : int) returns (#res.base : int, #res.offset : int){
    var #t~mem0 : int;
    var #t~mem3 : int;
    var #t~short4 : bool;
    var #t~mem5 : int;
    var #t~short6 : bool;
    var #t~post2 : int;
    var ~k~0 : int;
    var #t~pre1 : int;
    var ~l~0 : int;
    var #t~post7 : int;
    var #t~post8 : int;
    var ~k.base : int, ~k.offset : int;
    var ~i~0 : int;

  $Ultimate##0:
    ~k.base, ~k.offset := #in~k.base, #in~k.offset;
    call #t~mem0 := read~int(~k.base, ~k.offset, 4);
    ~i~0 := #t~mem0;
    havoc #t~mem0;
    ~l~0 := 0;
    goto $Ultimate##1;
  $Ultimate##1:
    goto $Ultimate##2, $Ultimate##3;
  $Ultimate##2:
    assume true;
    goto $Ultimate##4, $Ultimate##5;
  $Ultimate##4:
    assume !(~l~0 < 3);
    goto $Ultimate##6;
  $Ultimate##5:
    assume !!(~l~0 < 3);
    call write~int(~l~0, ~#level~0.base, ~#level~0.offset + 4 * ~i~0, 4);
    call write~int(~i~0, ~#last_to_enter~0.base, ~#last_to_enter~0.offset + 4 * ~l~0, 4);
    ~k~0 := 0;
    goto $Ultimate##7;
  $Ultimate##7:
    goto $Ultimate##8, $Ultimate##9;
  $Ultimate##8:
    assume true;
    goto $Ultimate##10, $Ultimate##11;
  $Ultimate##10:
    assume !(~k~0 < 4);
    goto Loop~0;
  $Ultimate##11:
    assume !!(~k~0 < 4);
    goto $Ultimate##12;
  $Ultimate##12:
    goto $Ultimate##13, $Ultimate##14;
  $Ultimate##13:
    assume true;
    goto Loop~2;
  Loop~2:
    #t~short4 := ~k~0 != ~i~0;
    goto $Ultimate##15, $Ultimate##16;
  $Ultimate##15:
    assume #t~short4;
    call #t~mem3 := read~int(~#level~0.base, ~#level~0.offset + 4 * ~k~0, 4);
    #t~short4 := #t~mem3 >= ~l~0;
    goto $Ultimate##17;
  $Ultimate##16:
    assume !#t~short4;
    goto $Ultimate##17;
  $Ultimate##17:
    #t~short6 := #t~short4;
    goto $Ultimate##18, $Ultimate##19;
  $Ultimate##18:
    assume #t~short6;
    call #t~mem5 := read~int(~#last_to_enter~0.base, ~#last_to_enter~0.offset + 4 * ~l~0, 4);
    #t~short6 := #t~mem5 == ~i~0;
    goto $Ultimate##20;
  $Ultimate##19:
    assume !#t~short6;
    goto $Ultimate##20;
  $Ultimate##20:
    goto $Ultimate##21, $Ultimate##22;
  $Ultimate##21:
    assume !#t~short6;
    havoc #t~short4;
    havoc #t~short6;
    havoc #t~mem5;
    havoc #t~mem3;
    goto Loop~1;
  $Ultimate##22:
    assume !!#t~short6;
    havoc #t~short4;
    havoc #t~short6;
    havoc #t~mem5;
    havoc #t~mem3;
    ~fairness_label_num~0 := ~i~0;
    ~fairness_label_verified~0 := ~fairness_label_num~0;
    goto $Ultimate##12;
  $Ultimate##14:
    assume !true;
    goto Loop~1;
  Loop~1:
    #t~post2 := ~k~0;
    ~k~0 := 1 + #t~post2;
    havoc #t~post2;
    goto $Ultimate##7;
  $Ultimate##9:
    assume !true;
    goto Loop~0;
  Loop~0:
    #t~pre1 := 1 + ~l~0;
    ~l~0 := 1 + ~l~0;
    havoc #t~pre1;
    goto $Ultimate##1;
  $Ultimate##3:
    assume !true;
    goto $Ultimate##6;
  $Ultimate##6:
    #t~post7 := ~x~0;
    ~x~0 := 1 + #t~post7;
    call write~int(-1, ~#level~0.base, ~#level~0.offset + 4 * ~i~0, 4);
    #t~post8 := ~fairness_label_end~0;
    ~fairness_label_end~0 := 1 + #t~post8;
    #res.base, #res.offset := 0, 0;
    return;
}

