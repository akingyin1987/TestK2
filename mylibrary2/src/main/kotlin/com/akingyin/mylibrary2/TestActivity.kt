package com.akingyin.mylibrary2

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.akingyin.mylibrary.TestData


/**
 *
 * @author: aking <a href="mailto:akingyin@163.com">Contact me.</a>
 * @since: 2024/7/24 11:32
 * @version: 1.0
 */
class TestActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TestCompose()
        }

    }


    @Composable
    private fun TestCompose() {
        val testData = remember {
            TestData()
        }
       TestInterface(testData = testData)
    }


}

@Composable
fun TestInterface(testData: TestData){
    Text(text = "测试名称2=>${testData.name}")
}
