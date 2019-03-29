package com.gmail.parusovvadim.remountreciveraudio;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    @Test
    public void addition_isCorrect() {
        assertEquals(4, 2 + 2);
    }

    @Test
    public void Run() {
//
//        String msg = "В работе были рассмотрены основные способы измерения антенн и выявлены их преимущества и недостатки. Самым предпочтительным является способ измерения в ближней зоне, так как он позволяет производить измерения характеристик антенн на малых расстояниях и не имеет зависимости от погодных условий. Однако в рамках классических узкополосных методов измерения ближней зоны есть ряд недостатков, которые можно компенсировать применением сверхширокополосных импульсных сигналов, которые позволяют производить измерения такими же способами, как и в узкополосных методах. Однако они позволяют повысить точность измерений за счет временной селекции переотражений между измерительным зондом, измеряемой антенной и элементами конструкции сканера.";
//        String msgLat = "THIS SOFTWARE IS PROVIDED BY YOURKIT \"AS IS\" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL YOURKIT BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.";
//        msg =  msg + msgLat;

//       String msg = "Rauf Faik - Я люблю тебя";
        String msg = "Океан Ельзи - Обійми" + (char)1112;
        String m = "іi";


//            String msg = "Я";
//        for (int i = 0; i < 1000000; i++) {
        String res = TransliteraterAUDI.Transliterate(msg);

//        }
//        String exp = "A JA";
        String exp = "Rauf Faik - JA ljublju tebja";
        assertEquals(exp, res);
    }
}