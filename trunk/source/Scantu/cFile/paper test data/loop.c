//@ ltl invariant positive: [](AP(d == 0) ==> <>AP(d == 5));
// #include <stdio.h>
int d = 0;
int main() {
  for (int i = 0; i < 2; i++)
  {
    d++;
  }
  return 0;
}
