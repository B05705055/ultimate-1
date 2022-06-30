var ~d~0 : int;

procedure ULTIMATE.start() returns ();
modifies ~d~0;

implementation ULTIMATE.start() returns (){
    var #t~ret2 : int;
    var main_#res : int;
    var main_#t~post1 : int;
    var main_#t~post0 : int;
    var main_~i~0 : int;

  $Ultimate##0:
    assume { :begin_inline_ULTIMATE.init } true;
    ~d~0 := 0;
    goto ULTIMATE.init_returnLabel;
  ULTIMATE.init_returnLabel:
    assume { :end_inline_ULTIMATE.init } true;
    assume { :begin_inline_main } true;
    havoc main_#res;
    havoc main_#t~post1, main_#t~post0, main_~i~0;
    main_~i~0 := 0;
    goto $Ultimate##1;
  $Ultimate##1:
    goto $Ultimate##2, $Ultimate##3;
  $Ultimate##2:
    assume true;
    goto $Ultimate##4, $Ultimate##5;
  $Ultimate##4:
    assume !(main_~i~0 < 3);
    goto $Ultimate##6;
  $Ultimate##5:
    assume !!(main_~i~0 < 3);
    main_#t~post1 := ~d~0;
    ~d~0 := 1 + main_#t~post1;
    goto main_Loop~0;
  main_Loop~0:
    main_#t~post0 := main_~i~0;
    main_~i~0 := 1 + main_#t~post0;
    havoc main_#t~post0;
    goto $Ultimate##1;
  $Ultimate##3:
    assume !true;
    goto $Ultimate##6;
  $Ultimate##6:
    main_#res := 0;
    goto main_returnLabel;
  main_returnLabel:
    #t~ret2 := main_#res;
    assume { :end_inline_main } true;
    return;
}

