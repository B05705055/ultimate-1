//@ ltl invariant positive: [](AP(x == 0) ==> <>AP(x == 1));

int x = 0;
int main() {
	while(1)
	{
		x = 0;
	}
}