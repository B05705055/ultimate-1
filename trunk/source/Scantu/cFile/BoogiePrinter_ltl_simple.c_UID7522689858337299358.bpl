var ~x~0 : int;

procedure increaseX(#in~n : int) returns ();
modifies ~x~0;

implementation increaseX(#in~n : int) returns (){
    var #t~post0 : int;
    var ~n : int;

  $Ultimate##0:
    ~n := #in~n;
    goto $Ultimate##1, $Ultimate##2;
  $Ultimate##1:
    assume 0 == ~n;
    return;
  $Ultimate##2:
    assume !(0 == ~n);
    #t~post0 := ~x~0;
    ~x~0 := 1 + #t~post0;
    call increaseX(~n - 1);
    return;
}

procedure ULTIMATE.start() returns ();
modifies ~x~0;

implementation ULTIMATE.start() returns (){
    var #t~ret1 : int;
    var main_#res : int;

  $Ultimate##0:
    assume { :begin_inline_ULTIMATE.init } true;
    ~x~0 := 0;
    goto ULTIMATE.init_returnLabel;
  ULTIMATE.init_returnLabel:
    assume { :end_inline_ULTIMATE.init } true;
    assume { :begin_inline_main } true;
    havoc main_#res;
    call increaseX(10);
    goto main_returnLabel;
  main_returnLabel:
    #t~ret1 := main_#res;
    assume { :end_inline_main } true;
    return;
}

