//@ ltl invariant positive: [](AP(x == 0) ==> <>AP(x == 4));

#include <stdio.h>
#include <pthread.h>
#include <assert.h>
#define N 4
typedef unsigned long int pthread_t;

int x = 0;
int number[N];
int entering[N];

int largest(int arr[], int n)
{
    int i;
    // Initialize maximum element 
    int max = arr[0];
    for (i = 1; i < n; i++)
        if (arr[i] > max)
            max = arr[i];
    return max;
}



void *f(int n){
        //printf("Starting thread %d\n", n);
        entering[n] = 1;
        number[n] = 1 + largest(number, N);
        entering[n] = 0;

        for(int j = 0; j < N; j++)
        {
                while(entering[j]){}
                while ( number[j] != 0  &&
                ( number[j] < number[n] || ( number[j] == number[n] && j < n ) ) ){}
        }

        /* critical section */
        int y = x;
        y++;
        x = y;

        number[n] = 0;

        //printf("Ending thread %d\n", n);
}

int main (){

    pthread_t t0, t1, t2, t3;
    pthread_create(&t0, 0, f, 0);
    pthread_create(&t1, 0, f, 1);
    pthread_create(&t2, 0, f, 2);
    pthread_create(&t3, 0, f, 3);
    pthread_join(t0, 0);
    pthread_join(t1, 0);
    pthread_join(t2, 0);
    pthread_join(t3, 0);

    // printf("x %d\n", x);
    return 0;
}

