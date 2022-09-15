const #funAddr~thr.base : int;
const #funAddr~thr.offset : int;
axiom #funAddr~thr.base == -1 && #funAddr~thr.offset == 0;
type ~pthread_t~0 = int;
var ~#flag~0.base : int, ~#flag~0.offset : int;

var ~turn~0 : int;

var ~x~0 : int;

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
modifies #pthreadsForks, #valid, #memory_int, #length, #NULL.base, #NULL.offset, ~#flag~0.base, ~#flag~0.offset, ~turn~0, ~x~0, ~#thr_num_array~0.base, ~#thr_num_array~0.offset, ~fairness_label_num~0, ~fairness_label_verified~0, ~fairness_label_end~0;

implementation ULTIMATE.start() returns (){
    var #t~ret11 : int;
    var main_#res : int;
    var main_#t~post4 : int;
    var main_~i~1 : int;
    var main_#t~pre5 : int;
    var main_#t~nondet6 : int;
    var main_#t~pre7 : int;
    var main_#t~nondet8 : int;
    var main_#t~mem9 : int;
    var main_#t~mem10 : int;
    var main_~#t0~0.base : int, main_~#t0~0.offset : int;
    var main_~#t1~0.base : int, main_~#t1~0.offset : int;

  $Ultimate##0:
    assume { :begin_inline_ULTIMATE.init } true;
    #NULL.base, #NULL.offset := 0, 0;
    #valid := #valid[0 := 0];
    call ~#flag~0.base, ~#flag~0.offset := #Ultimate.allocOnStack(8);
    call write~init~int(0, ~#flag~0.base, ~#flag~0.offset, 4);
    call write~init~int(0, ~#flag~0.base, 4 + ~#flag~0.offset, 4);
    ~turn~0 := 0;
    ~x~0 := 0;
    call ~#thr_num_array~0.base, ~#thr_num_array~0.offset := #Ultimate.allocOnStack(8);
    call write~init~int(0, ~#thr_num_array~0.base, ~#thr_num_array~0.offset, 4);
    call write~init~int(0, ~#thr_num_array~0.base, 4 + ~#thr_num_array~0.offset, 4);
    ~fairness_label_num~0 := 0;
    ~fairness_label_verified~0 := 0;
    ~fairness_label_end~0 := 0;
    goto ULTIMATE.init_returnLabel;
  ULTIMATE.init_returnLabel:
    assume { :end_inline_ULTIMATE.init } true;
    assume { :begin_inline_main } true;
    havoc main_#res;
    havoc main_#t~post4, main_~i~1, main_#t~pre5, main_#t~nondet6, main_#t~pre7, main_#t~nondet8, main_#t~mem9, main_#t~mem10, main_~#t0~0.base, main_~#t0~0.offset, main_~#t1~0.base, main_~#t1~0.offset;
    main_~i~1 := 0;
    goto $Ultimate##1;
  $Ultimate##1:
    goto $Ultimate##2, $Ultimate##3;
  $Ultimate##2:
    assume true;
    goto $Ultimate##4, $Ultimate##5;
  $Ultimate##4:
    assume !(main_~i~1 < 2);
    goto $Ultimate##6;
  $Ultimate##5:
    assume !!(main_~i~1 < 2);
    call write~int(main_~i~1, ~#thr_num_array~0.base, ~#thr_num_array~0.offset + 4 * main_~i~1, 4);
    goto main_Loop~1;
  main_Loop~1:
    main_#t~post4 := main_~i~1;
    main_~i~1 := 1 + main_#t~post4;
    havoc main_#t~post4;
    goto $Ultimate##1;
  $Ultimate##3:
    assume !true;
    goto $Ultimate##6;
  $Ultimate##6:
    call main_~#t0~0.base, main_~#t0~0.offset := #Ultimate.allocOnStack(8);
    call main_~#t1~0.base, main_~#t1~0.offset := #Ultimate.allocOnStack(8);
	
	
    
	main_#t~pre5 := #pthreadsForks;
    #pthreadsForks := 1 + #pthreadsForks;
    call write~int(main_#t~pre5, main_~#t0~0.base, main_~#t0~0.offset, 8);
    fork main_#t~pre5 thr(~#thr_num_array~0.base, ~#thr_num_array~0.offset);
    
	
	main_#t~pre7 := #pthreadsForks;
    #pthreadsForks := 1 + #pthreadsForks;
    call write~int(main_#t~pre7, main_~#t1~0.base, main_~#t1~0.offset, 8);
    fork main_#t~pre7 thr(~#thr_num_array~0.base, 4 + ~#thr_num_array~0.offset);
    
	
	
	call main_#t~mem9 := read~int(main_~#t0~0.base, main_~#t0~0.offset, 8);
    join main_#t~pre5;
    call main_#t~mem10 := read~int(main_~#t1~0.base, main_~#t1~0.offset, 8);
    join main_#t~pre7;
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
    #t~ret11 := main_#res;
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
modifies ~turn~0, ~fairness_label_num~0, ~fairness_label_verified~0, ~x~0, ~fairness_label_end~0, #memory_int;

implementation thr(#in~k.base : int, #in~k.offset : int) returns (#res.base : int, #res.offset : int){
    var #t~mem0 : int;
    var #t~mem1 : int;
    var #t~post2 : int;
    var #t~post3 : int;
    var ~k.base : int, ~k.offset : int;
    var ~i~0 : int;
    var ~y~0 : int;

  $Ultimate##0:
    ~k.base, ~k.offset := #in~k.base, #in~k.offset;
    call #t~mem0 := read~int(~k.base, ~k.offset, 4);
    ~i~0 := #t~mem0;
    havoc #t~mem0;
    call write~int(1, ~#flag~0.base, ~#flag~0.offset + 4 * ~i~0, 4);
    ~turn~0 := 1 - ~i~0;
    goto $Ultimate##1;
  $Ultimate##1:
    goto $Ultimate##2, $Ultimate##3;
  $Ultimate##2:
    assume true;
    goto Loop~0;
  Loop~0:
    call #t~mem1 := read~int(~#flag~0.base, ~#flag~0.offset + 4 * (1 - ~i~0), 4);
    goto $Ultimate##4, $Ultimate##5;
  $Ultimate##4:
    assume !(1 == #t~mem1 && ~turn~0 == 1 - ~i~0);
    havoc #t~mem1;
    goto $Ultimate##6;
  $Ultimate##5:
    assume !!(1 == #t~mem1 && ~turn~0 == 1 - ~i~0);
    havoc #t~mem1;
    ~fairness_label_num~0 := 0;
    ~fairness_label_verified~0 := ~fairness_label_num~0;
    goto $Ultimate##1;
  $Ultimate##3:
    assume !true;
    goto $Ultimate##6;
  $Ultimate##6:
    ~y~0 := 0;
    ~y~0 := ~x~0;
    #t~post2 := ~y~0;
    ~y~0 := 1 + #t~post2;
    ~x~0 := ~y~0;
    call write~int(0, ~#flag~0.base, ~#flag~0.offset + 4 * ~i~0, 4);
    #t~post3 := ~fairness_label_end~0;
    ~fairness_label_end~0 := 1 + #t~post3;
    #res.base, #res.offset := 0, 0;
    return;
}

